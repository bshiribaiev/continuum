const BACKEND_URL = "http://localhost:8080";
const USER_ID_KEY = "continuumUserId";
const WORKSPACE_ID_KEY = "continuumWorkspaceId";
let lastIngestedText = null; // simple guard to avoid duplicate task memories
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

// Ensure we have a stable workspace/project for this user (scopes context)
async function getWorkspaceId(userId) {
  return new Promise((resolve) => {
    chrome.storage.sync.get([WORKSPACE_ID_KEY], async (result) => {
      if (result[WORKSPACE_ID_KEY]) {
        resolve(result[WORKSPACE_ID_KEY]);
        return;
      }

      try {
        const resp = await fetch(`${BACKEND_URL}/api/workspaces`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            name: "ChatGPT Project",
            ownerId: userId,
            description: "Default project created by the ChatGPT extension",
          }),
        });
        const data = await resp.json();
        const workspaceId = data.id;
        chrome.storage.sync.set({ [WORKSPACE_ID_KEY]: workspaceId }, () =>
          resolve(workspaceId)
        );
      } catch (e) {
        console.error("[Continuum] failed to create workspace", e);
        resolve(null);
      }
    });
  });
}

async function transformPrompt(rawText) {
  console.log("[Continuum] transformPrompt called with:", rawText);
  const userId = await getUserId();
  const workspaceId = await getWorkspaceId(userId);

  // Detect if this already looks like a woven Continuum prompt
  const looksLikeWovenPrompt =
    rawText.includes("===== CONTEXT START =====") ||
    rawText.includes("You are an AI assistant that uses a persistent memory layer (Continuum)");

  if (looksLikeWovenPrompt) {
    console.log("[Continuum] skipping ingestion; text already looks like woven prompt");
  } 
  else if (rawText !== lastIngestedText) {
    console.log("[Continuum] ingesting new task memory");
    await fetch(`${BACKEND_URL}/api/ingestion/messages`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        userId,
        workspaceId,
        source: "chatgpt_extension",
        text: rawText,
        topic: "chat",
        tags: null,
        importance: 3,
      }),
    });
    lastIngestedText = rawText;
  } else {
    console.log("[Continuum] skipping ingestion for duplicate task text");
  }

  // 2) Generate woven prompt
  const resp = await fetch(`${BACKEND_URL}/api/prompts/generate`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      userId,
      workspaceId,
      task: rawText,
      contextLimit: 10,
      includeSystemInstructions: true,
    }),
  });

  const data = await resp.json();
  return data.prompt || rawText;
}

function hookChatGPT() {
  console.log("[Continuum] hookChatGPT running");

  let busy = false;
  let readyToSend = false; // after transform, let the next send through

  async function handleSend(source) {
    if (busy) return;
    busy = true;
    try {
      // Always re-query the current input
      const textarea =
        document.querySelector("#prompt-textarea") || // main ChatGPT input
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
      
      // Notify React/ProseMirror that the content changed
      try {
        const inputEvent =
          typeof InputEvent === "function"
            ? new InputEvent("input", { bubbles: true, cancelable: true })
            : new Event("input", { bubbles: true, cancelable: true });

        textarea.dispatchEvent(inputEvent);
        console.log("[Continuum] dispatched input event");
      } catch (e) {
        console.warn("[Continuum] failed to dispatch input event:", e);
      }

      // Give React a moment to process the input event and update its state
      await new Promise((resolve) => setTimeout(resolve, 50));

      console.log("[Continuum] prompt transformed, ready to send");
      readyToSend = true; // next Enter/click will send without re-transforming
    } catch (err) {
      console.error("[Continuum] error in handleSend:", err);
    } finally {
      busy = false;
    }
  }

  // Intercept ANY Enter (no Shift) on the page (capture)
  document.addEventListener(
    "keydown",
    (e) => {
      if (e.key === "Enter" && !e.shiftKey) {
        // If already transformed, let it send
        if (readyToSend) {
          console.log("[Continuum] Enter: sending transformed prompt");
          readyToSend = false;
          return; // let the event through
        }
        console.log("[Continuum] Enter detected, calling handleSend");
        e.preventDefault();
        handleSend("keydown");
      }
    },
    true
  );

  // Intercept pointerdown on the send button (capture) - fires BEFORE click
  document.addEventListener(
    "pointerdown",
    (e) => {
      const btn = e.target.closest(
        'button[data-testid="send-button"], #composer-submit-button'
      );
      if (!btn) return;

      // If already transformed, let it send
      if (readyToSend) {
        console.log("[Continuum] pointerdown: sending transformed prompt");
        readyToSend = false;
        return; // let the event through
      }

      console.log("[Continuum] send button pointerdown detected");

      // Prevent ChatGPT's own handlers from sending the original prompt
      e.preventDefault();
      e.stopImmediatePropagation();

      // Transform the prompt (don't auto-send)
      handleSend("pointerdown");
    },
    true
  );

  console.log("[Continuum] Input hooks attached");
}
// Wait a bit for ChatGPT UI to render
setTimeout(hookChatGPT, 2000);