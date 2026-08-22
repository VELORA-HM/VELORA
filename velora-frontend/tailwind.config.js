/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        velora: {
          bg: "#0B0D10",
          card: "#12161C",
          border: "#202732",
          gold: "#D4AF37",
          goldHover: "#E5C158",
          muted: "#8E9AA8",
          cream: "#F4EFEA",
        }
      },
      fontFamily: {
        serif: ["Playfair Display", "Cinzel", "serif"],
        sans: ["Inter", "system-ui", "sans-serif"],
      }
    },
  },
  plugins: [],
}
