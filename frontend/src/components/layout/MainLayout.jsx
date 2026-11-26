import { useState } from 'react';
import SiteHeader from './SiteHeader';
import SiteFooter from './SiteFooter';

export default function MainLayout({ children, member, loggingOut, onLogout, notice, onDismissNotice, isHome = false, isChat = false, pastelBackground = false }) {
  const [globalNotice, setGlobalNotice] = useState('');

  const banner = notice || globalNotice;

  return (
    <div className={`main-page${isHome ? ' is-home' : ''}${isChat ? ' is-chat' : ''}${pastelBackground ? ' has-pastel-background' : ''}`}>
      <SiteHeader member={member} loggingOut={loggingOut} onLogout={onLogout} />

      {banner && (
        <div className="mp-global-notice" role="status">
          {banner}
          <button type="button" onClick={() => (notice ? onDismissNotice?.() : setGlobalNotice(''))} aria-label="알림 닫기">×</button>
        </div>
      )}

      <div className="mp-body">
        <div className="mp-content">
          <main className="mp-main">{children}</main>
          <SiteFooter />
        </div>
      </div>
    </div>
  );
}
