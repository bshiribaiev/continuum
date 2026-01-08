const BACKEND_URL = "http://localhost:8080/api";
const USER_ID_KEY = "continuumUserId";
const WORKSPACE_ID_KEY = "continuumWorkspaceId";

console.log("[Continuum] content script loaded");

// User & Workspace ID helpers 
function getUserId() {
  return new Promise((resolve) => {
    chrome.storage.sync.get([USER_ID_KEY], async (result) => {
      if (result[USER_ID_KEY]) {
        resolve(result[USER_ID_KEY]);
        return;
      }
      try {
        const resp = await fetch(`${BACKEND_URL}/users`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ name: "ChatGPT User" }),
        });
        const data = await resp.json();
        const odcUid = data.id;
        chrome.storage.sync.set({ [USER_ID_KEY]: odcUid }, () => resolve(odcUid));
      } catch (e) {
        console.error("[Continuum] failed to create user", e);
        resolve(null);
      }
    });
  });
}

async function getWorkspaceId(userId) {
  return new Promise((resolve) => {
    chrome.storage.sync.get([WORKSPACE_ID_KEY], async (result) => {
      if (result[WORKSPACE_ID_KEY]) {
        resolve(result[WORKSPACE_ID_KEY]);
        return;
      }
      try {
        const resp = await fetch(`${BACKEND_URL}/workspaces`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            name: "ChatGPT Project",
            ownerId: userId,
            description: "Default project created by the ChatGPT extension",
          }),
        });
        const data = await resp.json();
        chrome.storage.sync.set({ [WORKSPACE_ID_KEY]: data.id }, () => resolve(data.id));
      } catch (e) {
        console.error("[Continuum] failed to create workspace", e);
        resolve(null);
      }
    });
  });
}

// API helpers
async function saveToMemory(text) {
  const userId = await getUserId();
  const workspaceId = await getWorkspaceId(userId);
  
  await fetch(`${BACKEND_URL}/ingestion/messages`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      userId,
      workspaceId,
      source: "chatgpt_extension",
      text,
      topic: "chat",
      importance: 3,
    }),
  });
  console.log("[Continuum] Saved to memory");
}

async function getEnhancedPrompt(text) {
  const userId = await getUserId();
  const workspaceId = await getWorkspaceId(userId);
  
  const resp = await fetch(`${BACKEND_URL}/prompts/generate`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      userId,
      workspaceId,
      task: text,
      contextLimit: 10,
      includeSystemInstructions: true,
    }),
  });
  const data = await resp.json();
  return data.prompt || text;
}

// Overlay Button UI
function createOverlayButton() {
  const existing = document.getElementById("continuum-overlay");
  if (existing) existing.remove();

  const container = document.createElement("div");
  container.id = "continuum-overlay";
  container.innerHTML = `
    <style>
      #continuum-overlay {
        position: fixed;
        bottom: 100px;
        right: 30px;
        z-index: 10000;
        font-family: -apple-system, BlinkMacSystemFont, sans-serif;
      }
      #continuum-btn {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: white;
        border: none;
        padding: 12px 16px;
        border-radius: 12px;
        cursor: pointer;
        font-size: 14px;
        font-weight: 600;
        box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
        transition: all 0.2s ease;
        display: flex;
        align-items: center;
        gap: 8px;
      }
      #continuum-btn:hover {
        transform: translateY(-2px);
        box-shadow: 0 6px 20px rgba(102, 126, 234, 0.5);
      }
      #continuum-menu {
        display: none;
        position: absolute;
        bottom: 55px;
        right: 0;
        background: white;
        border-radius: 12px;
        box-shadow: 0 4px 20px rgba(0,0,0,0.15);
        overflow: hidden;
        min-width: 180px;
      }
      #continuum-menu.show { display: block; }
      .continuum-menu-item {
        padding: 12px 16px;
        cursor: pointer;
        font-size: 13px;
        color: #333;
        transition: background 0.15s;
        border-bottom: 1px solid #eee;
      }
      .continuum-menu-item:last-child { border-bottom: none; }
      .continuum-menu-item:hover { background: #f5f5f5; }
      .continuum-menu-item .icon { margin-right: 8px; }
    </style>
    <div id="continuum-menu">
      <div class="continuum-menu-item" data-action="enhance">
        <span class="icon">✨</span> Enhance Prompt
      </div>
      <div class="continuum-menu-item" data-action="save">
        <span class="icon">💾</span> Save to Memory
      </div>
      <div class="continuum-menu-item" data-action="both">
        <span class="icon">🚀</span> Enhance + Save
      </div>
    </div>
    <button id="continuum-btn">
      <span>✨</span> Continuum
    </button>
  `;

  document.body.appendChild(container);

  // Toggle menu
  const btn = document.getElementById("continuum-btn");
  const menu = document.getElementById("continuum-menu");
  
  btn.addEventListener("click", () => {
    menu.classList.toggle("show");
  });

  // Close menu when clicking outside
  document.addEventListener("click", (e) => {
    if (!container.contains(e.target)) {
      menu.classList.remove("show");
    }
  });

  // Menu actions
  menu.addEventListener("click", async (e) => {
    const action = e.target.closest(".continuum-menu-item")?.dataset.action;
    if (!action) return;
    
    menu.classList.remove("show");
    
    // Find ChatGPT's textarea
    const textarea = document.querySelector('textarea[data-id="root"]') 
                  || document.querySelector("#prompt-textarea")
                  || document.querySelector('div[contenteditable="true"]');
    
    if (!textarea) {
      console.error("[Continuum] Could not find input area");
      return;
    }

    const currentText = textarea.value || textarea.innerText || textarea.textContent;
    if (!currentText.trim()) {
      console.log("[Continuum] No text to process");
      return;
    }

    btn.disabled = true;
    btn.innerHTML = '<span>⏳</span> Working...';

    try {
      if (action === "save") {
        await saveToMemory(currentText);
        showToast("Saved to memory! 💾");
      } else if (action === "enhance") {
        const enhanced = await getEnhancedPrompt(currentText);
        setTextAreaValue(textarea, enhanced);
        showToast("Prompt enhanced! ✨");
      } else if (action === "both") {
        await saveToMemory(currentText);
        const enhanced = await getEnhancedPrompt(currentText);
        setTextAreaValue(textarea, enhanced);
        showToast("Saved & enhanced! 🚀");
      }
    } catch (err) {
      console.error("[Continuum] Error:", err);
      showToast("Error - check console", true);
    }

    btn.disabled = false;
    btn.innerHTML = '<span>✨</span> Continuum';
  });
}

function setTextAreaValue(textarea, value) {
  if (textarea.tagName === "TEXTAREA") {
    textarea.value = value;
    textarea.dispatchEvent(new Event("input", { bubbles: true }));
  } else {
    // contenteditable div
    textarea.innerText = value;
    textarea.dispatchEvent(new InputEvent("input", { bubbles: true }));
  }
}

function showToast(message, isError = false) {
  const toast = document.createElement("div");
  toast.style.cssText = `
    position: fixed;
    bottom: 160px;
    right: 30px;
    background: ${isError ? "#ef4444" : "#10b981"};
    color: white;
    padding: 12px 20px;
    border-radius: 8px;
    font-size: 14px;
    z-index: 10001;
    animation: fadeInOut 2s ease forwards;
  `;
  toast.textContent = message;
  
  const style = document.createElement("style");
  style.textContent = `
    @keyframes fadeInOut {
      0% { opacity: 0; transform: translateY(10px); }
      15% { opacity: 1; transform: translateY(0); }
      85% { opacity: 1; transform: translateY(0); }
      100% { opacity: 0; transform: translateY(-10px); }
    }
  `;
  document.head.appendChild(style);
  document.body.appendChild(toast);
  
  setTimeout(() => toast.remove(), 2000);
}

// Initialize
function init() {
  // Wait for ChatGPT to load
  const checkReady = setInterval(() => {
    const textarea = document.querySelector('textarea[data-id="root"]') 
                  || document.querySelector("#prompt-textarea")
                  || document.querySelector('div[contenteditable="true"]');
    if (textarea) {
      clearInterval(checkReady);
      createOverlayButton();
      console.log("[Continuum] Overlay button injected");
    }
  }, 500);
}

init();