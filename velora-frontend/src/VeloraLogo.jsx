import React from 'react';

export default function VeloraLogo({ size = 38, className = "" }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 100 100"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      className={`transition-all duration-300 drop-shadow-[0_0_12px_rgba(212,175,55,0.25)] ${className}`}
    >
      <defs>
        <linearGradient id="veloraGoldGrad" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stopColor="#FFF2C6" />
          <stop offset="35%" stopColor="#E5C158" />
          <stop offset="70%" stopColor="#D4AF37" />
          <stop offset="100%" stopColor="#8C6E1E" />
        </linearGradient>

        <radialGradient id="veloraInnerGlow" cx="50%" cy="50%" r="50%">
          <stop offset="0%" stopColor="#D4AF37" stopOpacity="0.25" />
          <stop offset="100%" stopColor="#D4AF37" stopOpacity="0" />
        </radialGradient>
      </defs>

      <circle cx="50" cy="50" r="44" fill="url(#veloraInnerGlow)" />

      <polygon
        points="50,4 94,50 50,96 6,50"
        stroke="url(#veloraGoldGrad)"
        strokeWidth="1.8"
        strokeLinecap="round"
        strokeLinejoin="round"
      />

      <polygon
        points="50,11 88,50 50,89 12,50"
        stroke="url(#veloraGoldGrad)"
        strokeWidth="0.75"
        strokeOpacity="0.5"
      />

      <circle cx="50" cy="4" r="1.8" fill="#FFF2C6" />
      <circle cx="94" cy="50" r="1.8" fill="#FFF2C6" />
      <circle cx="50" cy="96" r="1.8" fill="#FFF2C6" />
      <circle cx="6" cy="50" r="1.8" fill="#FFF2C6" />

      <path
        d="M32 30 L50 72 L68 30"
        stroke="url(#veloraGoldGrad)"
        strokeWidth="3.2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />

      <path
        d="M40 30 L50 53 L60 30"
        stroke="url(#veloraGoldGrad)"
        strokeWidth="1.4"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeOpacity="0.8"
      />

      <polygon
        points="50,22 52,26 56,26 53,29 54,33 50,30 46,33 47,29 44,26 48,26"
        fill="url(#veloraGoldGrad)"
      />
    </svg>
  );
}