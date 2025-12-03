const BACKEND_URL = "http://localhost:8080";
const USER_ID_KEY = "continuumUserId";
console.log("[Continuum] content script loaded");

// Ensure we have a stable userId for this browser
async function getUserId() {
  return new Promise((resolve) => {
    chrome.storage.sync.get([USER_ID_KEY], async (result) => {
      if (result[USER_ID_KEY]) {
        resolve(result[USER_ID_KEY]);
        return;
      }

      // Create a new user in Continuum
      const username = "ext-" + Math.random().toString(36).slice(2, 10);
      const resp = await fetch(`${BACKEND_URL}/api/users`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          username,
          email: `${username}@example.com`,
          displayName: "Browser Extension User"
        })
      });
      const data = await resp.json();
      const userId = data.id;
      chrome.storage.sync.set({ [USER_ID_KEY]: userId }, () => resolve(userId));
    });
  });
}

async function transformPrompt(rawText) {
    console.log("[Continuum] transformPrompt called with:", rawText);
    const userId = await getUserId();

    // 1) Ingest the message as a TASK
    await fetch(`${BACKEND_URL}/api/ingestion/messages`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
        userId,
        source: "chatgpt_extension",
        text: rawText,
        type: "TASK",
        topic: "chat",
        tags: null,
        importance: 3
        })
    });

    // 2) Generate woven prompt
    const resp = await fetch(`${BACKEND_URL}/api/prompts/generate`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
        userId,
        task: rawText,
        contextLimit: 10,
        includeSystemInstructions: true
        })
    });

  const data = await resp.json();
  return data.prompt || rawText;
}

function hookChatGPT() {
    console.log("[Continuum] hookChatGPT running");
  
    document.addEventListener(
      "keydown",
      (e) => {
        console.log("[Continuum] keydown:", e.key, "target:", e.target);
      },
      true // capture phase
    );
    
    document.addEventListener("click", (e) => {
        const btn = e.target.closest('button[data-testid="send-button"], #composer-submit-button');
        if (!btn) return;
        console.log("[Continuum] send button click detected");
        e.preventDefault();
        handleSend("click");
      });
    console.log("[Continuum] Input hooks attached");
  
    let busy = false;
  
    async function handleSend(source) {
      if (busy) return;
      busy = true;
      try {
        // Try to find the main input each time
        const textarea =
          document.querySelector("textarea") ||
          document.querySelector('div[contenteditable="true"]');
  
        if (!textarea) {
          console.warn("[Continuum] No input element found");
          busy = false;
          return;
        }
  
        const isTextarea = textarea.tagName && textarea.tagName.toLowerCase() === "textarea";
        const original = (isTextarea ? textarea.value : textarea.textContent).trim();
  
        if (!original) {
          busy = false;
          return;
        }
  
        console.log("[Continuum] handleSend from", source, "with:", original);
  
        const woven = await transformPrompt(original);
  
        // Replace user text with woven prompt
        if (isTextarea) {
          textarea.value = woven;
        } else {
          textarea.textContent = woven;
        }
  
        // Click ChatGPT's send button
        const sendButton =
          document.querySelector('button[data-testid="send-button"]') ||
          document.querySelector("#composer-submit-button");
  
        if (sendButton) {
          sendButton.click();
        } else {
          console.warn("[Continuum] No send button found; cannot auto-send.");
        }
      } catch (err) {
        console.error("[Continuum] error in handleSend:", err);
      } finally {
        busy = false;
      }
    }
  
    // Intercept ANY Enter (no Shift) on the page for now
    document.addEventListener("keydown", (e) => {
      if (e.key === "Enter" && !e.shiftKey) {
        e.preventDefault();
        handleSend("keydown");
      }
    });
  
    // Intercept clicks on the send button
    document.addEventListener("click", (e) => {
      const btn = e.target.closest('button[data-testid="send-button"], #composer-submit-button');
      if (!btn) return;
      e.preventDefault();
      handleSend("click");
    });
  
    console.log("[Continuum] Input hooks attached");
  }
// Wait a bit for ChatGPT UI to render
setTimeout(hookChatGPT, 2000);