from fastapi import FastAPI
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer
import joblib

app = FastAPI()

# Load embedding model + trained classifier once at startup
embed_model = SentenceTransformer("all-MiniLM-L6-v2")
clf = joblib.load("models/intent_clf.joblib")

class IntentRequest(BaseModel):
    text: str

class IntentResponse(BaseModel):
    type: str

@app.post("/classify-intent", response_model=IntentResponse)
def classify_intent(req: IntentRequest):
    emb = embed_model.encode([req.text])  # shape (1, d)
    pred = clf.predict(emb)[0]
    return IntentResponse(type=pred)