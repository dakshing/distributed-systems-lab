import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  // Ramp up to 50 virtual users over 10 seconds, then hold for 20s
  stages: [
    { duration: '10s', target: 100 },
    { duration: '20s', target: 100 },
    { duration: '5s', target: 0 },
  ],
};

const BASE_URL = 'http://localhost:8080';

export default function () {
  const randomId = Math.random().toString(36).substring(7);
  const payload = `https://google.com/search?q=${randomId}`;

  const params = {
    headers: {
      'Content-Type': 'text/plain',
    }
  };

  const res = http.post(`${BASE_URL}/shorten`, payload, params);


  check(res, {
    'is status 200': (r) => r.status === 200,
    'latency < 100ms': (r) => r.timings.duration < 100,
  });

  if (res.status === 200) {
      const shortUrl = res.body;

      const readRes = http.get(shortUrl, { redirects: 0 }); // Don't follow the redirect, just check the 302

      check(readRes, {
          'is redirect 302': (r) => r.status === 302,
      });
  }

  sleep(0.1);
}