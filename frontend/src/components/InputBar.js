import { useState } from "react";

// Bottom bar — text input + send button
function InputBar({ onSend, isLoading }) {

    // tracks what the user is currently typing
    const [input, setInput] = useState("");

    // called when user clicks Send or presses Enter
    function handleSend() {
        if (input.trim() === "") return;
        onSend(input);
        setInput("");
    }

    // allows pressing Enter to send instead of clicking button
    function handleKeyDown(e) {
        if (e.key === "Enter") handleSend();
    }

    return (
        <div style={{
            display: "flex",
            padding: "16px",
            gap: "12px",
            backgroundColor: "#ffffff",
            borderTop: "1px solid #e0e8f0"
        }}>
            <input
                type="text"
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyDown={handleKeyDown}
                placeholder="Ask a question about Hughes installation..."
                disabled={isLoading}
                style={{
                    flex: 1,
                    padding: "12px 16px",
                    borderRadius: "24px",
                    border: "1px solid #c0d4e8",
                    fontSize: "14px",
                    outline: "none",
                    backgroundColor: isLoading ? "#f0f0f0" : "#ffffff"
                }}
            />
            <button
                onClick={handleSend}
                disabled={isLoading}
                style={{
                    backgroundColor: isLoading ? "#a0b4c8" : "#003087",
                    color: "#ffffff",
                    border: "none",
                    borderRadius: "24px",
                    padding: "12px 24px",
                    fontSize: "14px",
                    cursor: isLoading ? "not-allowed" : "pointer"
                }}
            >
                {isLoading ? "..." : "Send"}
            </button>
        </div>
    );
}

export default InputBar;