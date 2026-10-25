import React, { useState, useEffect, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';

const API = import.meta.env.VITE_API_URL || 'http://localhost:8080';

const today = () => new Date().toISOString().slice(0, 10);
const inr = (n) => '₹' + Number(n).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

const defaultSuites = [
  {
    roomNumber: 101,
    tier: 'Standard',
    name: 'Executive Studio Suite',
    price: 10000.0,
    isBooked: false,
    image: 'https://images.unsplash.com/photo-1590490360182-c33d57733427?auto=format&fit=crop&w=1200&q=80',
    description: 'A minimalist sanctuary featuring acoustic soundproofing, ergonomic workstation, high-speed fiber Wi-Fi, and rainfall marble shower.',
    amenities: ['Fiber Wi-Fi', 'Smart 4K TV', 'Queen Bed', 'Espresso Bar']
  },
  {
    roomNumber: 201,
    tier: 'Deluxe',
    name: 'Skyline Panorama Suite',
    price: 18500.0,
    isBooked: false,
    image: 'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?auto=format&fit=crop&w=1200&q=80',
    description: 'Floor-to-ceiling panoramic glass offering breathtaking skyline vistas, complimentary champagne breakfast, and deep-soaking bath.',
    amenities: ['Skyline Balcony', 'Gourmet Breakfast', 'King Size Bed', 'Marble Bath']
  },
  {
    roomNumber: 301,
    tier: 'Penthouse',
    name: 'Imperial Presidential Sanctuary',
    price: 55000.0,
    isBooked: false,
    image: 'https://images.unsplash.com/photo-1618773928121-c32242e63f39?auto=format&fit=crop&w=1200&q=80',
    description: 'The pinnacle of bespoke luxury. Private rooftop infinity jacuzzi, 24/7 dedicated butler, curated mini-bar tab, and private elevator access.',
    amenities: ['Private Butler', 'Curated Mini-Bar', 'Infinity Spa', 'Private Elevators']
  }
];

const inputClass =
  'w-full bg-black/50 border border-[#1E2530] rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:border-[#D4AF37]';
const labelClass = 'block text-[11px] uppercase tracking-wider text-zinc-400 mb-1';

const statusStyle = {
  CONFIRMED: 'text-emerald-300 border-emerald-400/30 bg-emerald-400/10',
  CHECKED_OUT: 'text-zinc-400 border-zinc-500/30 bg-zinc-500/10',
  CANCELLED: 'text-red-300 border-red-400/30 bg-red-400/10'
};

async function api(path, options) {
  const res = await fetch(API + path, options);
  const data = await res.json();
  if (!res.ok || data.status === 'ERROR') throw new Error(data.message || 'Request failed');
  return data;
}

export default function App() {
  const [suites, setSuites] = useState(defaultSuites);
  const [online, setOnline] = useState(null); // null = checking
  const [selectedSuite, setSelectedSuite] = useState(null);
  const [form, setForm] = useState({});
  const [quote, setQuote] = useState(null);
  const [formError, setFormError] = useState('');
  const [activeBooking, setActiveBooking] = useState(null);
  const [loading, setLoading] = useState(false);
  const [bookings, setBookings] = useState([]);
  const [stats, setStats] = useState([]);
  const [busyRef, setBusyRef] = useState(null);

  const refresh = useCallback(async () => {
    try {
      const [s, b, st] = await Promise.all([api('/api/suites'), api('/api/bookings'), api('/api/stats')]);
      if (Array.isArray(s) && s.length > 0) setSuites(s);
      setBookings(b);
      setStats(st);
      setOnline(true);
    } catch (err) {
      console.warn('Java server not reachable:', err.message);
      setOnline(false);
    }
  }, []);

  useEffect(() => {
    refresh();
  }, [refresh]);

  // Ask the Java Room subclass for the bill whenever the suite or nights change
  useEffect(() => {
    if (!selectedSuite || !online) return;
    const n = Number(form.nights);
    if (!n || n < 1) return;
    const t = setTimeout(() => {
      api(`/api/quote?room=${selectedSuite.roomNumber}&nights=${n}`)
        .then(setQuote)
        .catch(() => setQuote(null));
    }, 250);
    return () => clearTimeout(t);
  }, [selectedSuite, form.nights, online]);

  const openSuite = (suite) => {
    setSelectedSuite(suite);
    setForm({ name: '', phone: '', email: '', checkIn: today(), nights: 1, paymentMethod: 'UPI', paymentDetail: '' });
    setQuote(null);
    setFormError('');
  };

  const update = (key) => (e) => setForm((f) => ({ ...f, [key]: e.target.value }));

  const handleBookingSubmit = async (e) => {
    e.preventDefault();
    setFormError('');
    setLoading(true);
    try {
      const data = await api('/api/reserve', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name: form.name,
          phone: form.phone,
          email: form.email || null,
          roomNumber: selectedSuite.roomNumber,
          checkIn: form.checkIn,
          nights: Number(form.nights),
          paymentMethod: form.paymentMethod,
          paymentDetail: form.paymentMethod === 'CASH' ? null : form.paymentDetail
        })
      });
      setActiveBooking({
        bookingId: data.bookingId,
        guestName: form.name,
        roomNumber: selectedSuite.roomNumber,
        tier: selectedSuite.tier,
        checkIn: data.checkIn,
        checkOut: data.checkOut,
        nights: form.nights,
        paymentMethod: form.paymentMethod,
        totalTariff: data.total
      });
      setSelectedSuite(null);
      refresh();
    } catch (err) {
      setFormError(
        err.message === 'Failed to fetch'
          ? 'Could not reach the Velora server. Start VeloraServer and try again.'
          : err.message
      );
    } finally {
      setLoading(false);
    }
  };

  const handleCheckout = async (ref) => {
    setBusyRef(ref);
    try {
      await api('/api/checkout', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ bookingRef: ref })
      });
      await refresh();
    } catch (err) {
      alert(err.message);
    } finally {
      setBusyRef(null);
    }
  };

  const totalRevenue = stats.reduce((sum, s) => sum + Number(s.revenue), 0);
  const occupied = suites.filter((s) => s.isBooked).length;

  return (
    <div className="min-h-screen bg-[#07090E] text-[#EAEAEA] font-sans antialiased [font-variant-numeric:lining-nums] selection:bg-[#D4AF37] selection:text-black">
      {/* Top Navigation */}
      <nav className="border-b border-[#1E2530] px-8 py-5 flex items-center justify-between backdrop-blur-md sticky top-0 z-40 bg-[#07090E]/80">
        <div className="flex items-center space-x-3 cursor-pointer" onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })}>
          <div className="w-8 h-8 rounded-full border border-[#D4AF37] flex items-center justify-center text-[#D4AF37] font-serif font-bold text-lg">
            V
          </div>
          <div>
            <h1 className="text-sm font-semibold tracking-[0.25em] text-[#D4AF37] uppercase">VELORA</h1>
            <p className="text-[9px] tracking-[0.3em] text-zinc-500 uppercase">Grand Resort</p>
          </div>
        </div>

        <div className="hidden md:flex space-x-8 text-xs tracking-widest uppercase text-zinc-400">
          <a href="#suites" className="hover:text-[#D4AF37] transition">Suites</a>
          <a href="#ledger" className="hover:text-[#D4AF37] transition">Reservations Ledger</a>
        </div>

        <div className="flex items-center gap-4">
          <span className="hidden sm:flex items-center gap-2 text-[10px] tracking-widest uppercase text-zinc-500">
            <span
              className={`w-2 h-2 rounded-full ${
                online === null ? 'bg-zinc-500' : online ? 'bg-emerald-400 shadow-[0_0_8px_#34d399]' : 'bg-red-500'
              }`}
            />
            {online === null ? 'Connecting' : online ? 'MySQL live' : 'Server offline'}
          </span>
          <button
            onClick={() => openSuite(suites.find((s) => !s.isBooked) || suites[0])}
            className="border border-[#D4AF37]/40 hover:border-[#D4AF37] text-[#D4AF37] text-[11px] tracking-widest uppercase px-5 py-2 rounded-full transition duration-300"
          >
            Reserve Stay
          </button>
        </div>
      </nav>

      {online === false && (
        <div className="bg-red-500/10 border-b border-red-500/30 text-red-200 text-xs text-center py-2.5 px-4">
          The Java server is not running, so bookings cannot be saved. Start <span className="font-mono">VeloraServer</span> on
          port 8080, then reload.
        </div>
      )}

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
      <main id="suites" className="max-w-6xl mx-auto px-6 pb-24 grid grid-cols-1 md:grid-cols-3 gap-8 scroll-mt-24">
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
                <div className="absolute top-4 left-4 bg-black/70 backdrop-blur-md px-3 py-1 rounded-full border border-white/10 text-[10px] tracking-widest uppercase text-zinc-300">
                  {suite.tier}
                </div>
                <div className="absolute top-4 right-4 bg-black/70 backdrop-blur-md px-3 py-1 rounded-full border border-white/10 text-xs font-serif text-[#D4AF37]">
                  ₹{Number(suite.price).toLocaleString('en-IN')} <span className="text-[10px] text-zinc-400 font-sans">/night</span>
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
                onClick={() => openSuite(suite)}
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

      {/* Reservations Ledger: everything below is read back from MySQL */}
      <section id="ledger" className="max-w-6xl mx-auto px-6 pb-28 scroll-mt-24">
        <div className="flex items-end justify-between flex-wrap gap-4 mb-8">
          <div>
            <p className="text-xs tracking-[0.4em] text-[#D4AF37] uppercase mb-2">Live from velora_db</p>
            <h2 className="text-3xl md:text-4xl font-serif text-white">Reservations Ledger</h2>
          </div>
          <button
            onClick={refresh}
            className="text-[11px] tracking-widest uppercase text-zinc-400 hover:text-[#D4AF37] border border-[#1E2530] hover:border-[#D4AF37]/50 px-4 py-2 rounded-full transition"
          >
            Refresh
          </button>
        </div>

        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
          {[
            ['Reservations', bookings.length],
            ['Suites occupied', `${occupied} / ${suites.length}`],
            ['Revenue', inr(totalRevenue)],
            ['Top tier', stats[0] && Number(stats[0].revenue) > 0 ? stats[0].tier : '—']
          ].map(([label, value]) => (
            <div key={label} className="bg-[#0D1117] border border-[#1E2530] rounded-2xl p-5">
              <p className="text-[10px] tracking-widest uppercase text-zinc-500">{label}</p>
              <p className="text-2xl font-serif text-white mt-2 [font-variant-numeric:lining-nums]">{value}</p>
            </div>
          ))}
        </div>

        <div className="bg-[#0D1117] border border-[#1E2530] rounded-2xl overflow-x-auto">
          <table className="w-full text-xs min-w-[900px] [&_td]:whitespace-nowrap [font-variant-numeric:lining-nums]">
            <thead>
              <tr className="text-left text-[10px] tracking-widest uppercase text-zinc-500 border-b border-[#1E2530]">
                <th className="px-4 py-4 font-medium">Ref</th>
                <th className="px-4 py-4 font-medium">Guest</th>
                <th className="px-4 py-4 font-medium">Suite</th>
                <th className="px-4 py-4 font-medium">Stay</th>
                <th className="px-4 py-4 font-medium">Payment</th>
                <th className="px-4 py-4 font-medium text-right">Total</th>
                <th className="px-4 py-4 font-medium">Status</th>
                <th className="px-4 py-4" />
              </tr>
            </thead>
            <tbody>
              {bookings.length === 0 && (
                <tr>
                  <td colSpan={8} className="px-5 py-10 text-center text-zinc-500">
                    {online ? 'No reservations yet. Reserve a suite above and it will appear here.' : 'Start the Java server to load reservations.'}
                  </td>
                </tr>
              )}
              {bookings.map((b) => (
                <tr key={b.bookingRef} className="border-b border-[#1E2530]/60 last:border-0 hover:bg-white/[0.02]">
                  <td className="px-4 py-4 font-mono text-[#D4AF37]">{b.bookingRef}</td>
                  <td className="px-4 py-4">
                    <div className="text-white">{b.guestName}</div>
                    <div className="text-zinc-500 font-mono text-[11px]">{b.phone}</div>
                  </td>
                  <td className="px-4 py-4">
                    <div className="text-white">#{b.roomNumber} · {b.tier}</div>
                    <div className="text-zinc-500">{b.suiteName}</div>
                  </td>
                  <td className="px-4 py-4 text-zinc-300">
                    {b.checkIn} → {b.checkOut}
                    <div className="text-zinc-500">{b.nights} night(s)</div>
                  </td>
                  <td className="px-4 py-4 text-zinc-300">
                    {b.paymentMethod || '—'}
                    <div className="text-zinc-500 font-mono text-[11px]">{b.paymentReference || ''}</div>
                  </td>
                  <td className="px-4 py-4 text-right font-serif text-sm text-white">{inr(b.total)}</td>
                  <td className="px-4 py-4">
                    <span className={`text-[10px] tracking-wider px-2.5 py-1 rounded-full border ${statusStyle[b.status] || ''}`}>
                      {b.status.replace('_', ' ')}
                    </span>
                  </td>
                  <td className="px-4 py-4 text-right">
                    {b.status === 'CONFIRMED' && (
                      <button
                        onClick={() => handleCheckout(b.bookingRef)}
                        disabled={busyRef === b.bookingRef}
                        className="text-[10px] tracking-widest uppercase border border-[#D4AF37]/40 hover:border-[#D4AF37] text-[#D4AF37] px-3 py-1.5 rounded-full transition"
                      >
                        {busyRef === b.bookingRef ? '…' : 'Check out'}
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      {/* Reservation Modal */}
      <AnimatePresence>
        {selectedSuite && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm overflow-y-auto">
            <motion.div
              initial={{ scale: 0.95, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.95, opacity: 0 }}
              className="bg-[#0D1117] border border-[#1E2530] rounded-2xl p-8 max-w-lg w-full shadow-2xl relative my-8"
            >
              <h3 className="text-xl font-serif text-white">Reserve {selectedSuite.name}</h3>
              <p className="text-xs text-zinc-400 mt-1 mb-6">
                Suite #{selectedSuite.roomNumber} • {selectedSuite.tier} • ₹{Number(selectedSuite.price).toLocaleString('en-IN')}/night
              </p>

              <form onSubmit={handleBookingSubmit} className="space-y-4">
                <div>
                  <label className={labelClass}>Guest Full Name</label>
                  <input type="text" required value={form.name} onChange={update('name')} placeholder="Full name" className={inputClass} />
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className={labelClass}>Contact Phone</label>
                    <input
                      type="tel"
                      required
                      pattern="\d{10}"
                      title="10-digit mobile number"
                      value={form.phone}
                      onChange={update('phone')}
                      placeholder="10-digit mobile"
                      className={inputClass}
                    />
                  </div>
                  <div>
                    <label className={labelClass}>Email (optional)</label>
                    <input type="email" value={form.email} onChange={update('email')} placeholder="name@mail.com" className={inputClass} />
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className={labelClass}>Check-in</label>
                    <input type="date" required min={today()} value={form.checkIn} onChange={update('checkIn')} className={`${inputClass} [color-scheme:dark]`} />
                  </div>
                  <div>
                    <label className={labelClass}>Nights</label>
                    <input type="number" min="1" max="30" required value={form.nights} onChange={update('nights')} className={inputClass} />
                  </div>
                </div>

                <div>
                  <label className={labelClass}>Payment</label>
                  <div className="grid grid-cols-3 gap-2">
                    {['UPI', 'CARD', 'CASH'].map((m) => (
                      <button
                        key={m}
                        type="button"
                        onClick={() => setForm((f) => ({ ...f, paymentMethod: m, paymentDetail: '' }))}
                        className={`py-2 rounded-xl text-[11px] tracking-widest uppercase border transition ${
                          form.paymentMethod === m
                            ? 'border-[#D4AF37] text-[#D4AF37] bg-[#D4AF37]/10'
                            : 'border-[#1E2530] text-zinc-400 hover:border-zinc-600'
                        }`}
                      >
                        {m === 'CARD' ? 'Card' : m === 'UPI' ? 'UPI' : 'Cash'}
                      </button>
                    ))}
                  </div>
                  {form.paymentMethod === 'UPI' && (
                    <input type="text" required value={form.paymentDetail} onChange={update('paymentDetail')} placeholder="UPI ID, e.g. name@okaxis" className={`${inputClass} mt-2`} />
                  )}
                  {form.paymentMethod === 'CARD' && (
                    <input
                      type="text"
                      required
                      inputMode="numeric"
                      pattern="\d{4}"
                      maxLength={4}
                      title="Last 4 digits only"
                      value={form.paymentDetail}
                      onChange={update('paymentDetail')}
                      placeholder="Last 4 digits of card (only these are stored)"
                      className={`${inputClass} mt-2`}
                    />
                  )}
                  {form.paymentMethod === 'CASH' && <p className="text-[11px] text-zinc-500 mt-2">Pay at the front desk on arrival.</p>}
                </div>

                <div className="pt-4 border-t border-[#1E2530] space-y-1.5 text-xs">
                  <div className="flex justify-between text-zinc-500">
                    <span>Calculated by</span>
                    <span className="font-mono text-zinc-400">{quote ? `${quote.javaClass}.calculateTariff(${form.nights})` : '—'}</span>
                  </div>
                  <div className="flex justify-between text-zinc-500">
                    <span>Rule</span>
                    <span className="text-zinc-400">{quote ? quote.rule : selectedSuite.tariffRule || '—'}</span>
                  </div>
                  <div className="flex justify-between items-center pt-1">
                    <span className="text-zinc-400">Total Settlement:</span>
                    <span className="text-base font-serif text-[#D4AF37] [font-variant-numeric:lining-nums]">{quote ? inr(quote.total) : online ? '…' : '—'}</span>
                  </div>
                </div>

                {formError && (
                  <div className="text-xs text-red-300 bg-red-500/10 border border-red-500/30 rounded-xl px-4 py-3">{formError}</div>
                )}

                <div className="flex gap-3 pt-2">
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
                    className="w-1/2 py-3 rounded-xl text-xs uppercase tracking-widest font-semibold bg-[#D4AF37] hover:bg-[#c49f2e] text-black disabled:opacity-60"
                  >
                    {loading ? 'Saving to MySQL…' : 'Confirm Stay'}
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
              <p className="text-[10px] tracking-[0.3em] text-[#D4AF37] uppercase">Saved to MySQL · velora_db</p>
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
                  <span className="text-zinc-500">Stay:</span>
                  <span>{activeBooking.checkIn} → {activeBooking.checkOut}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-zinc-500">Payment:</span>
                  <span>{activeBooking.paymentMethod}</span>
                </div>
                <div className="flex justify-between border-t border-[#1E2530] pt-2 text-sm">
                  <span className="text-zinc-400">Total Settled:</span>
                  <span className="text-[#D4AF37] font-bold">{inr(activeBooking.totalTariff)}</span>
                </div>
              </div>

              <button
                onClick={() => {
                  setActiveBooking(null);
                  document.getElementById('ledger')?.scrollIntoView({ behavior: 'smooth' });
                }}
                className="w-full bg-[#D4AF37] text-black py-3 rounded-xl text-xs uppercase tracking-widest font-semibold hover:bg-[#c49f2e] transition"
              >
                View in Ledger
              </button>
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </div>
  );
}
