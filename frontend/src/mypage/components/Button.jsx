export default function Button({ children, variant = 'primary', className = '', type = 'button', ...props }) {
  return (
    <button type={type} className={[variant, className].filter(Boolean).join(' ')} {...props}>
      {children}
    </button>
  );
}
