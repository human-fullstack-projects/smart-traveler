import React from 'react';
import { createRoot } from 'react-dom/client';
import App from './App.jsx';
import { AuthProvider } from './contexts/AuthContext';
import './styles/tokens.css';
import './styles/global.css';
import './chat.css';

createRoot(document.getElementById('root')).render(
  <React.StrictMode><AuthProvider><App /></AuthProvider></React.StrictMode>,
);
