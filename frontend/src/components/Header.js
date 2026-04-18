// Top bar of the chat app — shows Hughes branding
function Header() {
    return (
        <div style={{
            backgroundColor: "#003087",
            padding: "16px 24px",
            display: "flex",
            alignItems: "center",
            gap: "12px"
        }}>
            {/* Hughes satellite icon */}
            <span style={{ fontSize: "24px" }}>🛰️</span>

            <div>
                <div style={{
                    color: "#ffffff",
                    fontSize: "18px",
                    fontWeight: "600"
                }}>
                    Hughes Field Assistant
                </div>
                <div style={{
                    color: "#a8c4e0",
                    fontSize: "12px"
                }}>
                    Powered by RAG + OpenAI
                </div>
            </div>
        </div>
    );
}

export default Header;