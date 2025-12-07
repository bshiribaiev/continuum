const BACKEND_URL = "http://localhost:8080";
const USER_ID_KEY = "continuumUserId";
const WORKSPACE_ID_KEY = "continuumWorkspaceId";

async function getUserId() {
  return new Promise((resolve) => {
    chrome.storage.sync.get([USER_ID_KEY], async (result) => {
      if (result[USER_ID_KEY]) {
        resolve(result[USER_ID_KEY]);
        return;
      }

      try {
        const username = "ext-" + Math.random().toString(36).slice(2, 10);
        const resp = await fetch(`${BACKEND_URL}/api/users`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            username,
            email: `${username}@example.com`,
            displayName: "Browser Extension User",
          }),
        });
        const data = await resp.json();
        const userId = data.id;
        chrome.storage.sync.set({ [USER_ID_KEY]: userId }, () => resolve(userId));
      } catch (e) {
        console.error("[Continuum popup] failed to create user", e);
        resolve(null);
      }
    });
  });
}

async function getActiveWorkspaceId() {
  return new Promise((resolve) => {
    chrome.storage.sync.get([WORKSPACE_ID_KEY], (result) => {
      resolve(result[WORKSPACE_ID_KEY] || null);
    });
  });
}

async function setActiveWorkspaceId(workspaceId) {
  return new Promise((resolve) => {
    chrome.storage.sync.set({ [WORKSPACE_ID_KEY]: workspaceId }, () => resolve());
  });
}

async function fetchWorkspaces(userId) {
  const resp = await fetch(
    `${BACKEND_URL}/api/workspaces?ownerId=${encodeURIComponent(userId)}`,
    {
      method: "GET",
    }
  );
  if (!resp.ok) {
    throw new Error(`Failed to load workspaces: ${resp.status}`);
  }
  return await resp.json();
}

async function createWorkspace(userId, name) {
  const resp = await fetch(`${BACKEND_URL}/api/workspaces`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      name,
      ownerId: userId,
      description: "Created from the Continuum extension popup",
    }),
  });
  if (!resp.ok) {
    throw new Error(`Failed to create workspace: ${resp.status}`);
  }
  return await resp.json();
}

function renderProjects(workspaces, activeId) {
  const listEl = document.getElementById("project-list");
  const activeEl = document.getElementById("active-project");
  listEl.innerHTML = "";

  const active = workspaces.find((w) => w.id === activeId);
  activeEl.textContent = active ? active.name : "None selected";

  workspaces.forEach((w) => {
    const li = document.createElement("li");
    li.className = "project" + (w.id === activeId ? " active" : "");
    li.dataset.id = w.id;

    const nameSpan = document.createElement("span");
    nameSpan.className = "project-name";
    nameSpan.textContent = w.name;

    const badge = document.createElement("span");
    badge.className = "badge";
    badge.textContent = w.id === activeId ? "Active" : "Use";

    li.appendChild(nameSpan);
    li.appendChild(badge);

    li.addEventListener("click", async () => {
      await setActiveWorkspaceId(w.id);
      renderProjects(workspaces, w.id);
      const status = document.getElementById("status");
      status.textContent = "Active project updated.";
    });

    listEl.appendChild(li);
  });
}

async function init() {
  const status = document.getElementById("status");
  status.textContent = "Loading projects...";

  const userId = await getUserId();
  if (!userId) {
    status.textContent = "Could not initialize user. Check backend.";
    return;
  }

  try {
    const [workspaces, activeId] = await Promise.all([
      fetchWorkspaces(userId),
      getActiveWorkspaceId(),
    ]);

    renderProjects(workspaces, activeId);
    status.textContent = "";
  } catch (e) {
    console.error("[Continuum popup] error loading workspaces", e);
    status.textContent = "Failed to load projects. Is the backend running?";
  }

  const form = document.getElementById("new-project-form");
  const nameInput = document.getElementById("new-project-name");
  const createBtn = document.getElementById("create-project");

  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    const name = nameInput.value.trim();
    if (!name) return;

    createBtn.disabled = true;
    status.textContent = "Creating project...";
    try {
      const workspace = await createWorkspace(userId, name);

      // Refresh list
      const workspaces = await fetchWorkspaces(userId);
      await setActiveWorkspaceId(workspace.id);
      renderProjects(workspaces, workspace.id);

      nameInput.value = "";
      status.textContent = "Project created and set active.";
    } catch (err) {
      console.error("[Continuum popup] failed to create workspace", err);
      status.textContent = "Failed to create project.";
    } finally {
      createBtn.disabled = false;
    }
  });
}

document.addEventListener("DOMContentLoaded", init);


