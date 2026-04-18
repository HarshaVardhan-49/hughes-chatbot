import { useEffect, useRef } from "react";

// Displays the list of messages — both user and bot bubbles
function ChatWindow({ messages }) {

    // ref attached to a dummy div at the bottom of the chat
    // every time messages change we scroll to it automatically
    const bottomRef = useRef(null);

    useEffect(() => {
        bottomRef.current?.scrollIntoView({ behavior: "smooth" });
    }, [messages]);

    return (
        <div style={{
            flex: 1,
            overflowY: "auto",
            padding: "24px",
            display: "flex",
            flexDirection: "column",
            gap: "12px",
            backgroundColor: "#f5f8ff"
        }}>
            {messages.map((msg, index) => (
                <div key={index} style={{
                    display: "flex",
                    justifyContent: msg.sender === "user" ? "flex-end" : "flex-start"
                }}>
                    <div style={{
                        maxWidth: "70%",
                        padding: "12px 16px",
                        borderRadius: msg.sender === "user" ? "18px 18px 4px 18px" : "18px 18px 18px 4px",
                        backgroundColor: msg.sender === "user" ? "#003087" : "#ffffff",
                        color: msg.sender === "user" ? "#ffffff" : "#1a1a1a",
                        fontSize: "14px",
                        lineHeight: "1.5",
                        boxShadow: "0 1px 3px rgba(0,0,0,0.1)"
                    }}>
                        {msg.text}
                    </div>
                </div>
            ))}

            {/* invisible div at the bottom — we scroll to this on every new message */}
            <div ref={bottomRef} />
        </div>
    );
}

export default ChatWindow;