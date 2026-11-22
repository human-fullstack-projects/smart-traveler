import React from 'react';
import { createRoot } from 'react-dom/client';
import App from './App.jsx';
import { AuthProvider } from './contexts/AuthContext';
import './styles/tokens.css';
import './styles/global.css';
import './chat.css';
import './styles/theme.css';
import './styles/brand.css';

const paymentQuery = new URLSearchParams(window.location.search);
const reservationId = paymentQuery.get('stayReservationId');
const paymentResult = paymentQuery.get('stayPaymentResult');
if (/^\d+$/.test(reservationId || '') && ['success', 'failure'].includes(paymentResult)) {
  paymentQuery.delete('stayReservationId');
  paymentQuery.delete('stayPaymentResult');
  window.history.replaceState(null, '', window.location.pathname
    + '#/payments/' + reservationId + '/' + paymentResult + '?' + paymentQuery);
}

createRoot(document.getElementById('root')).render(
  <React.StrictMode><AuthProvider><App /></AuthProvider></React.StrictMode>,
);
