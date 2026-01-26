# Jules Client 🦑

Jules Client is a sleek, AI-powered web interface designed to interact with **Jules**, a Google AI coding agent. It provides a modern and intuitive experience for managing repositories, starting AI sessions, and approving code-generation plans.

![Jules UI](https://jules.google/squid.png)

## ✨ Features

- **🚀 Interactive Sessions**: Start AI-driven coding sessions with natural language prompts.
- **📁 Repository Management**: Seamlessly browse and select repositories to work on.
- **📝 Plan Approval**: Review and approve AI-generated plans before any code changes are applied.
- **🔄 Real-time Updates**: Polling mechanism to keep track of AI activities and session status.
- **🔑 Secure Configuration**: Easy API key setup with local persistence for quick access.
- **🎨 Premium UI**: A dark-themed, responsive interface built with Inter typography and smooth transitions.

## 🛠️ Tech Stack

- **Core**: [React 19](https://react.dev/)
- **Build Tool**: [Vite](https://vitejs.dev/)
- **Language**: [TypeScript](https://www.typescriptlang.org/)
- **Styling**: [Tailwind CSS](https://tailwindcss.com/) (CDN)
- **Icons**: [Lucide React](https://lucide.dev/)
- **Markdown**: [React Markdown](https://github.com/remarkjs/react-markdown)
- **AI Integration**: [@google/genai](https://www.npmjs.com/package/@google/genai)

## 🚀 Getting Started

### Prerequisites

- [Node.js](https://nodejs.org/) (v18 or higher)
- [npm](https://www.npmjs.com/) or [yarn](https://yarnpkg.com/)

### Installation

1. **Clone the repository**:
   ```bash
   git clone https://github.com/your-username/jules-client.git
   cd jules-client
   ```

2. **Install dependencies**:
   ```bash
   npm install
   ```

3. **Start the development server**:
   ```bash
   npm run dev
   ```

4. **Open in browser**:
   Navigate to `http://localhost:5173` (or the port shown in your terminal).

## ⚙️ Configuration

To use Jules Client, you will need an API key for the Jules agent.

1. Launch the application.
2. Enter your **Jules API Key** in the welcome screen.
3. The key is stored locally in your browser for future sessions.

## 🤝 Contributing

Contributions are welcome! Feel free to open an issue or submit a pull request.

## 📄 License

This project is licensed under the [Apache License 2.0](LICENSE).

## ⚖️ Legal Disclaimer

This project is an independent open-source interface and is **not affiliated with, endorsed by, or maintained by Google**.

- This software is a wrapper around Google-provided tools and APIs.
- The use of Google APIs is governed by [Google’s Terms of Service](https://policies.google.com/terms) and [Google AI Generative Terms of Service](https://policies.google.com/terms/generative-ai).
- No ownership is claimed over the underlying Google technologies.
