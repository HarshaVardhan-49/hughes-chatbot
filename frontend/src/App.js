import { useState } from "react";
import Header from "./components/Header";
import ChatWindow from "./components/ChatWindow";
import InputBar from "./components/InputBar";
import { askQuestion } from "./api/botApi";

function App() {

    // holds the full conversation history — list of {sender, text} objects
    const [messages, setMessages] = useState([
        { sender: "bot", text: "Hi! I am the Hughes Field Assistant. Ask me anything about installation, signal troubleshooting, or equipment setup." }
    ]);

    // true while waiting for bot response — disables input during this time
    const [isLoading, setIsLoading] = useState(false);

    // called by InputBar when user hits Send
    async function handleSend(question) {

        // add user message to chat immediately
        const userMessage = { sender: "user", text: question };
        setMessages(prev => [...prev, userMessage]);
        setIsLoading(true);

        try {
            // call Spring Boot POST /bot/ask
            const answer = await askQuestion(question);

            // add bot reply to chat
            const botMessage = { sender: "bot", text: answer };
            setMessages(prev => [...prev, botMessage]);

        } catch (error) {
            // if something goes wrong show error as bot message
            const errorMessage = { sender: "bot", text: "Sorry, something went wrong: " + error.message };
            setMessages(prev => [...prev, errorMessage]);

        } finally {
            setIsLoading(false);
        }
    }

    return (
        <div style={{
            display: "flex",
            flexDirection: "column",
            height: "100vh",
            fontFamily: "Inter, -apple-system, sans-serif"
        }}>
            <Header />
            <ChatWindow messages={messages} />
            <InputBar onSend={handleSend} isLoading={isLoading} />
        </div>
    );
}

export default App;