import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';

const defaultSuites = [
  {
    roomNumber: 101,
    tier: 'Standard',
    name: 'Executive Studio Suite',
    price: 120.0,
    isBooked: false,
    image: 'https://images.unsplash.com/photo-1590490360182-c33d57733427?auto=format&fit=crop&w=1200&q=80',
    description: 'A minimalist sanctuary featuring acoustic soundproofing, ergonomic workstation, high-speed fiber Wi-Fi, and rainfall marble shower.',
    amenities: ['Fiber Wi-Fi', 'Smart 4K TV', 'Queen Bed', 'Espresso Bar']
  },
  {
    roomNumber: 201,
    tier: 'Deluxe',
    name: 'Skyline Panorama Suite',
    price: 250.0,
    isBooked: false,
    image: 'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?auto=format&fit=crop&w=1200&q=80',
    description: 'Floor-to-ceiling panoramic glass offering breathtaking skyline vistas, complimentary champagne breakfast, and deep-soaking bath.',
    amenities: ['Skyline Balcony', 'Gourmet Breakfast', 'King Size Bed', 'Marble Bath']
  },
  {
    roomNumber: 301,
    tier: 'Penthouse',
    name: 'Imperial Presidential Sanctuary',
    price: 750.0,
    isBooked: false,
    image: 'https://images.unsplash.com/photo-1618773928121-c32242e63f39?auto=format&fit=crop&w=1200&q=80',
    description: 'The pinnacle of bespoke luxury. Private rooftop infinity jacuzzi, 24/7 dedicated butler, curated mini-bar tab, and private elevator access.',
    amenities: ['Private Butler', 'Curated Mini-Bar', 'Infinity Spa', 'Private Elevators']
  }
];

export default function App() {
  const [suites, setSuites] = useState(defaultSuites);
  const [selectedSuite, setSelectedSuite] = useState(null);
  const [guestName, setGuestName] = useState('');
  const [guestPhone, setGuestPhone] = useState('');
  const [nights, setNights] = useState(1);
  const [activeBooking, setActiveBooking] = useState(null);
  const [loading, setLoading] = useState(false);

  // Fetch real-time room availability from Java Backend on Mount
  useEffect(() => {
    fetch('http://localhost:8080/api/suites')
      .then((res) => {
        if (!res.ok) throw new Error('Network response not ok');
        return res.json();
      })
      .then((data) => {
        if (Array.isArray(data) && data.length > 0) {
          setSuites(data);
        }
      })
      .catch((err) => {
        console.warn('Java server not reachable yet, using local state fallback:', err.message);
      });
  }, []);

  const handleBookingSubmit = async (e) => {
    e.preventDefault();
    if (!guestName || !guestPhone || !selectedSuite) return;

    setLoading(true);

    const payload = {
      name: guestName,
      phone: guestPhone,
      roomNumber: selectedSuite.roomNumber,
      nights: Number(nights),
      tier: selectedSuite.tier,
      price: selectedSuite.price
    };

    try {
      const response = await fetch('http://localhost:8080/api/reserve', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });

      const data = await response.json();

      if (data.status === 'SUCCESS') {
        setActiveBooking({
          bookingId: data.bookingId,
          guestName,
          guestPhone,
          roomNumber: selectedSuite.roomNumber,
          tier: selectedSuite.tier,
          nights,
          totalTariff: data.total
        });

        // Mark room as occupied in state
        setSuites((prev) =>
          prev.map((s) =>
            s.roomNumber === selectedSuite.roomNumber ? { ...s, isBooked: true } : s
          )
        );
        setSelectedSuite(null);
      }
    } catch (err) {
      console.error('Reservation API Error:', err);
      // Fallback local display if server disconnected
      const subtotal = selectedSuite.price * nights;
      const total = selectedSuite.tier === 'Penthouse' ? subtotal * 1.18 : subtotal;
      setActiveBooking({
        bookingId: 'VEL-' + Math.floor(10000 + Math.random() * 90000),
        guestName,
        guestPhone,
        roomNumber: selectedSuite.roomNumber,
        tier: selectedSuite.tier,
        nights,
        totalTariff: total
      });
      setSuites((prev) =>
        prev.map((s) =>
          s.roomNumber === selectedSuite.roomNumber ? { ...s, isBooked: true } : s
        )
      );
      setSelectedSuite(null);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#07090E] text-[#EAEAEA] font-sans antialiased selection:bg-[#D4AF37] selection:text-black">
      {/* Top Navigation */}
      <nav className="border-b border-[#1E2530] px-8 py-5 flex items-center justify-between backdrop-blur-md sticky top-0 z-40 bg-[#07090E]/80">
        <div className="flex items-center space-x-3 cursor-pointer">
          <div className="w-8 h-8 rounded-full border border-[#D4AF37] flex items-center justify-center text-[#D4AF37] font-serif font-bold text-lg">
            V
          </div>
          <div>
            <h1 className="text-sm font-semibold tracking-[0.25em] text-[#D4AF37] uppercase">VELORA</h1>
            <p className="text-[9px] tracking-[0.3em] text-zinc-500 uppercase">Grand Resort</p>
          </div>
        </div>

        <div className="hidden md:flex space-x-8 text-xs tracking-widest uppercase text-zinc-400">
          <span className="hover:text-[#D4AF37] transition cursor-pointer">Sanctuary</span>
          <span className="hover:text-[#D4AF37] transition cursor-pointer">Experiences</span>
          <span className="hover:text-[#D4AF37] transition cursor-pointer">Concierge</span>
        </div>

        <button
          onClick={() => setSelectedSuite(suites[0])}
          className="border border-[#D4AF37]/40 hover:border-[#D4AF37] text-[#D4AF37] text-[11px] tracking-widest uppercase px-5 py-2 rounded-full transition duration-300"
        >
          Reserve Stay
        </button>
      </nav>

      {/* Hero Section */}
      <section className="px-6 py-20 max-w-6xl mx-auto text-center">
        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.8 }}>
          <p className="text-xs tracking-[0.4em] text-[#D4AF37] uppercase mb-4">Bespoke Luxury Protocol</p>
          <h2 className="text-4xl md:text-6xl font-serif text-white max-w-3xl mx-auto leading-tight">
            Unrivaled Elegance. Unforgettable Sanctuary.
          </h2>
          <p className="text-zinc-400 text-sm md:text-base max-w-xl mx-auto mt-6">
            Experience an elevated standard of private indulgence. Hand-tailored suites, penthouse lounges, and automated concierge reservations.
          </p>
        </motion.div>
      </section>

      {/* Suites Showcase */}
      <main className="max-w-6xl mx-auto px-6 pb-28 grid grid-cols-1 md:grid-cols-3 gap-8">
        {suites.map((suite) => (
          <motion.div
            key={suite.roomNumber}
            whileHover={{ y: -6 }}
            transition={{ duration: 0.3 }}
            className="bg-[#0D1117] border border-[#1E2530] rounded-2xl overflow-hidden flex flex-col justify-between shadow-xl"
          >
            <div>
              <div className="relative h-60 overflow-hidden">
                <img
                  src={suite.image}
                  alt={suite.name}
                  className="w-full h-full object-cover transition-transform duration-700 hover:scale-105"
                />
                <div className="absolute top-4 right-4 bg-black/70 backdrop-blur-md px-3 py-1 rounded-full border border-white/10 text-xs font-serif text-[#D4AF37]">
                  ${suite.price.toFixed(0)} <span className="text-[10px] text-zinc-400 font-sans">/night</span>
                </div>
              </div>

              <div className="p-6">
                <p className="text-[10px] tracking-widest text-zinc-500 uppercase font-mono">Suite #{suite.roomNumber}</p>
                <h3 className="text-xl font-serif text-white mt-1 mb-3">{suite.name}</h3>
                <p className="text-xs text-zinc-400 leading-relaxed mb-6">{suite.description}</p>

                <div className="flex flex-wrap gap-2 mb-4">
                  {suite.amenities?.map((amenity, idx) => (
                    <span key={idx} className="text-[10px] bg-white/5 border border-white/10 px-2.5 py-1 rounded-md text-zinc-300">
                      {amenity}
                    </span>
                  ))}
                </div>
              </div>
            </div>

            <div className="p-6 pt-0">
              <button
                disabled={suite.isBooked}
                onClick={() => {
                  setSelectedSuite(suite);
                  setGuestName('');
                  setGuestPhone('');
                  setNights(1);
                }}
                className={`w-full py-3 rounded-xl text-xs uppercase tracking-widest font-semibold transition duration-300 ${
                  suite.isBooked
                    ? 'bg-zinc-800 text-zinc-500 cursor-not-allowed border border-zinc-700'
                    : 'bg-white hover:bg-[#D4AF37] text-black hover:text-black font-semibold shadow-lg'
                }`}
              >
                {suite.isBooked ? 'Occupied' : 'Reserve Suite'}
              </button>
            </div>
          </motion.div>
        ))}
      </main>

      {/* Reservation Modal */}
      <AnimatePresence>
        {selectedSuite && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm">
            <motion.div
              initial={{ scale: 0.95, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.95, opacity: 0 }}
              className="bg-[#0D1117] border border-[#1E2530] rounded-2xl p-8 max-w-md w-full shadow-2xl relative"
            >
              <h3 className="text-xl font-serif text-white">Reserve {selectedSuite.name}</h3>
              <p className="text-xs text-zinc-400 mt-1 mb-6">Suite #{selectedSuite.roomNumber} • ${selectedSuite.price}/night</p>

              <form onSubmit={handleBookingSubmit} className="space-y-4">
                <div>
                  <label className="block text-[11px] uppercase tracking-wider text-zinc-400 mb-1">Guest Full Name</label>
                  <input
                    type="text"
                    required
                    value={guestName}
                    onChange={(e) => setGuestName(e.target.value)}
                    placeholder="Guneet Tak"
                    className="w-full bg-black/50 border border-[#1E2530] rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:border-[#D4AF37]"
                  />
                </div>

                <div>
                  <label className="block text-[11px] uppercase tracking-wider text-zinc-400 mb-1">Contact Phone</label>
                  <input
                    type="tel"
                    required
                    value={guestPhone}
                    onChange={(e) => setGuestPhone(e.target.value)}
                    placeholder="8824018801"
                    className="w-full bg-black/50 border border-[#1E2530] rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:border-[#D4AF37]"
                  />
                </div>

                <div>
                  <label className="block text-[11px] uppercase tracking-wider text-zinc-400 mb-1">Nights Duration</label>
                  <input
                    type="number"
                    min="1"
                    max="30"
                    required
                    value={nights}
                    onChange={(e) => setNights(e.target.value)}
                    className="w-full bg-black/50 border border-[#1E2530] rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:border-[#D4AF37]"
                  />
                </div>

                <div className="pt-4 border-t border-[#1E2530] flex justify-between items-center text-xs">
                  <span className="text-zinc-400">Total Settlement:</span>
                  <span className="text-base font-serif text-[#D4AF37]">
                    ${(selectedSuite.price * nights * (selectedSuite.tier === 'Penthouse' ? 1.18 : 1)).toFixed(2)}
                  </span>
                </div>

                <div className="flex gap-3 pt-4">
                  <button
                    type="button"
                    onClick={() => setSelectedSuite(null)}
                    className="w-1/2 py-3 rounded-xl text-xs uppercase tracking-widest font-semibold border border-[#1E2530] hover:bg-white/5 text-zinc-400"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={loading}
                    className="w-1/2 py-3 rounded-xl text-xs uppercase tracking-widest font-semibold bg-[#D4AF37] hover:bg-[#c49f2e] text-black"
                  >
                    {loading ? 'Processing...' : 'Confirm Stay'}
                  </button>
                </div>
              </form>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* Confirmation Folio Modal */}
      <AnimatePresence>
        {activeBooking && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/85 backdrop-blur-md">
            <motion.div
              initial={{ scale: 0.9, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.9, opacity: 0 }}
              className="bg-[#0D1117] border border-[#D4AF37]/30 rounded-2xl p-8 max-w-md w-full shadow-2xl text-center"
            >
              <div className="w-12 h-12 rounded-full border border-[#D4AF37] mx-auto flex items-center justify-center text-[#D4AF37] text-xl mb-4">
                ✓
              </div>
              <p className="text-[10px] tracking-[0.3em] text-[#D4AF37] uppercase">Reservation Confirmed & Logged</p>
              <h3 className="text-2xl font-serif text-white mt-1 mb-4">Guest Folio Created</h3>

              <div className="bg-black/60 border border-[#1E2530] rounded-xl p-4 text-left space-y-2 text-xs text-zinc-300 font-mono mb-6">
                <div className="flex justify-between border-b border-[#1E2530] pb-2">
                  <span className="text-zinc-500">Folio Ref:</span>
                  <span className="text-[#D4AF37]">{activeBooking.bookingId}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-zinc-500">Guest:</span>
                  <span>{activeBooking.guestName}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-zinc-500">Suite:</span>
                  <span>#{activeBooking.roomNumber} ({activeBooking.tier})</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-zinc-500">Duration:</span>
                  <span>{activeBooking.nights} Night(s)</span>
                </div>
                <div className="flex justify-between border-t border-[#1E2530] pt-2 text-sm">
                  <span className="text-zinc-400">Total Settled:</span>
                  <span className="text-[#D4AF37] font-bold">${Number(activeBooking.totalTariff).toFixed(2)}</span>
                </div>
              </div>

              <button
                onClick={() => setActiveBooking(null)}
                className="w-full bg-[#D4AF37] text-black py-3 rounded-xl text-xs uppercase tracking-widest font-semibold hover:bg-[#c49f2e] transition"
              >
                Return to Dashboard
              </button>
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </div>
  );
}