import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const errorRate = new Rate('errors');
const bookCreationDuration = new Trend('book_creation_duration');

export const options = {
  stages: [
    { duration: '30s', target: 10 },   // ramp up
    { duration: '1m',  target: 10 },   // sustained load
    { duration: '30s', target: 0 },    // ramp down
  ],
  thresholds: {
    // Quality gates: pipeline fails if these are breached
    http_req_duration: ['p(95)<500'],   // 95th percentile under 500ms
    http_req_failed:   ['rate<0.01'],   // error rate under 1%
    errors:            ['rate<0.01'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

function randomIsbn() {
  return `978-${Math.floor(Math.random() * 9000000000) + 1000000000}`;
}

export default function () {
  // GET all books
  const listRes = http.get(`${BASE_URL}/api/books`);
  check(listRes, {
    'list status 200': (r) => r.status === 200,
    'list response time < 200ms': (r) => r.timings.duration < 200,
  });
  errorRate.add(listRes.status !== 200);

  // POST new book
  const payload = JSON.stringify({
    title: `Book ${Date.now()}`,
    author: 'Load Test Author',
    isbn: randomIsbn(),
    price: 29.99,
  });

  const createRes = http.post(`${BASE_URL}/api/books`, payload, {
    headers: { 'Content-Type': 'application/json' },
  });
  bookCreationDuration.add(createRes.timings.duration);

  check(createRes, {
    'create status 201': (r) => r.status === 201,
    'create response time < 500ms': (r) => r.timings.duration < 500,
  });
  errorRate.add(createRes.status !== 201);

  // GET created book
  if (createRes.status === 201) {
    const created = JSON.parse(createRes.body);
    const getRes = http.get(`${BASE_URL}/api/books/${created.id}`);
    check(getRes, {
      'get status 200': (r) => r.status === 200,
    });
    errorRate.add(getRes.status !== 200);
  }

  sleep(1);
}
