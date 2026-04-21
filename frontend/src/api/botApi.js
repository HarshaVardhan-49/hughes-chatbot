// Base URL of our Spring Boot backend
const BASE_URL = "http://98.92.65.102:8080";

// Sends the question to POST /bot/ask and returns the answer string
export async function askQuestion(question) {
    const response = await fetch(`${BASE_URL}/bot/ask`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({ question })
    });

    const data = await response.json();

    // If Spring Boot returned an error, throw it so the UI can handle it
    if (!response.ok) {
        throw new Error(data.error || "Something went wrong");
    }

    return data.answer;
}