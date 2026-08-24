import http from 'k6/http';
import { check } from 'k6';

const origins = {
  identity: __ENV.IDENTITY_URL || 'http://localhost:8081',
  shipment: __ENV.SHIPMENT_URL || 'http://localhost:8082',
  tracking: __ENV.TRACKING_URL || 'http://localhost:8083',
  routing: __ENV.ROUTING_URL || 'http://localhost:8084',
  notification: __ENV.NOTIFICATION_URL || 'http://localhost:8085',
  warehouse: __ENV.WAREHOUSE_URL || 'http://localhost:8086',
};

const names = [
  'identityHealth',
  'identityAuthBoundary',
  'shipmentHealth',
  'shipmentReadBoundary',
  'trackingHealth',
  'publicTrackingRead',
  'routingHealth',
  'notificationHealth',
  'warehouseHealth',
  'warehouseSearch',
];

function workload(exec) {
  const profile = __ENV.PROFILE || 'smoke';
  const rate = Number(__ENV.RATE || 3);
  if (profile === 'load') {
    return { exec, executor: 'constant-arrival-rate', rate, timeUnit: '1s', duration: '2m', preAllocatedVUs: 2, maxVUs: 20 };
  }
  if (profile === 'stress') {
    return { exec, executor: 'ramping-arrival-rate', startRate: rate, timeUnit: '1s', preAllocatedVUs: 5, maxVUs: 50, stages: [{ target: rate * 2, duration: '2m' }, { target: rate * 5, duration: '3m' }, { target: 0, duration: '1m' }] };
  }
  if (profile === 'spike') {
    return { exec, executor: 'ramping-arrival-rate', startRate: rate, timeUnit: '1s', preAllocatedVUs: 5, maxVUs: 80, stages: [{ target: rate * 10, duration: '15s' }, { target: rate * 10, duration: '45s' }, { target: rate, duration: '1m' }] };
  }
  if (profile === 'soak') {
    return { exec, executor: 'constant-arrival-rate', rate, timeUnit: '1s', duration: '30m', preAllocatedVUs: 2, maxVUs: 20 };
  }
  return { exec, executor: 'shared-iterations', vus: 1, iterations: 1, maxDuration: '30s' };
}

function thresholds(name, duration) {
  return {
    [`http_req_failed{scenario:${name}}`]: ['rate<0.01'],
    [`http_req_duration{scenario:${name}}`]: [`p(95)<${duration}`],
    [`checks{scenario:${name}}`]: ['rate>0.99'],
  };
}

export const options = {
  scenarios: Object.fromEntries(names.map((name) => [name, workload(name)])),
  thresholds: names.reduce((all, name) => Object.assign(all, thresholds(name, name.includes('Health') ? 500 : 800)), {}),
  discardResponseBodies: true,
  userAgent: 'urbanflow-quality-performance/0.1.0',
};

const expected = http.expectedStatuses({ min: 200, max: 399 }, 401, 404);

function get(url, accepted, label, params = {}) {
  const response = http.get(url, { ...params, responseCallback: expected, tags: { endpoint: label } });
  check(response, { [`${label} returns an expected status`]: (result) => accepted.includes(result.status) });
}

function health(origin, service) {
  get(`${origin}/actuator/health`, [200], `${service}.health`);
}

export function identityHealth() { health(origins.identity, 'identity'); }
export function identityAuthBoundary() { get(`${origins.identity}/api/v1/users/me`, [401], 'identity.auth-boundary'); }
export function shipmentHealth() { health(origins.shipment, 'shipment'); }
export function shipmentReadBoundary() {
  const headers = __ENV.ACCESS_TOKEN ? { Authorization: `Bearer ${__ENV.ACCESS_TOKEN}` } : {};
  get(`${origins.shipment}/api/v1/shipments/00000000-0000-0000-0000-000000000000`, [401, 404], 'shipment.read-boundary', { headers });
}
export function trackingHealth() { health(origins.tracking, 'tracking'); }
export function publicTrackingRead() { get(`${origins.tracking}/api/v1/tracking/${__ENV.TRACKING_NUMBER || 'UF-NOT-FOUND'}`, [200, 404], 'tracking.public-read'); }
export function routingHealth() { health(origins.routing, 'routing'); }
export function notificationHealth() { health(origins.notification, 'notification'); }
export function warehouseHealth() { health(origins.warehouse, 'warehouse'); }
export function warehouseSearch() { get(`${origins.warehouse}/api/v1/warehouses?page=0&size=20`, [200, 401], 'warehouse.search'); }
