import http from 'k6/http';
import { check, sleep } from 'k6';

export let options = {
  vus: 50,
  duration: '30s',
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500']
  }
};

const BASE = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
  const voterId = `user-${Math.floor(Math.random()*1000000)}`;
  const payload = JSON.stringify({
    pollId: 1,
    voterId,
    optionIds: [1]
  });
  const params = { headers: { 'Content-Type': 'application/json' } };
  const res = http.post(`${BASE}/api/votes/submit`, payload, params);
  check(res, {
    'status is 200': (r) => r.status === 200
  });
  sleep(0.1);
}
