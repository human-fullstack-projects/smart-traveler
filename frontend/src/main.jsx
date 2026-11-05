// import React from 'react';
// import { createRoot } from 'react-dom/client';
// import App from './App.jsx';
// <<<<<<< HEAD
// import './style.css';
// import './fixed-composer.css';
// import './shared/st-common.css';
// =======
// import { AuthProvider } from './contexts/AuthContext';
// import './styles/tokens.css';
// import './styles/global.css';
// import './chat.css';
// >>>>>>> origin/main

// createRoot(document.getElementById('root')).render(
//   <React.StrictMode><AuthProvider><App /></AuthProvider></React.StrictMode>,
// );
import React from 'react';
import { createRoot } from 'react-dom/client';
import App from './App.jsx';
import { AuthProvider } from './contexts/AuthContext';
import './styles/tokens.css';
import './styles/global.css';
import './chat.css';
import './styles/theme.css';

createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <AuthProvider>
      <App />
    </AuthProvider>
  </React.StrictMode>,
);