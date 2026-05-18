import { StrictMode, useState } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.jsx'
import AuthGate from './AuthGate.jsx'
import ProfilePage from './ProfilePage.jsx'

function RootApp() {
  const [currentPage, setCurrentPage] = useState('home')

  return (
    <AuthGate onOpenProfile={() => setCurrentPage('profile')}>
      {currentPage === 'profile' ? (
        <ProfilePage onBack={() => setCurrentPage('home')} />
      ) : (
        <App />
      )}
    </AuthGate>
  )
}

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <RootApp />
  </StrictMode>,
)