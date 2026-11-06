import http from 'k6/http';
import { Counter } from 'k6/metrics';

const allowedResponses = new Counter('rate_limit_allowed_responses');
const rejectedResponses = new Counter('rate_limit_rejected_responses');
const unavailableResponses = new Counter('rate_limit_unavailable_responses');
const unexpectedResponses = new Counter('rate_limit_unexpected_responses');

export const options = {
  scenarios: {
    gateway_rate_limit: {
      executor: 'constant-vus',
      vus: Number(__ENV.VUS || 100),
      duration: __ENV.DURATION || '2m',
      gracefulStop: '30s',
    },
  },
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
};

export default function () {
  const baseUrl = __ENV.BASE_URL;
  if (!baseUrl) {
    throw new Error('BASE_URL is required');
  }

  const pathTemplate = __ENV.PATH_TEMPLATE || '/api/orders';
  const requestPath = pathTemplate
    .replaceAll('{vu}', String(__VU))
    .replaceAll('{iteration}', String(__ITER));
  const method = (__ENV.METHOD || 'GET').toUpperCase();
  const headers = {
    Accept: 'application/json',
  };

  if (__ENV.AUTH_TOKEN) {
    headers[__ENV.AUTH_HEADER || 'Authorization'] = __ENV.AUTH_TOKEN;
  }
  if (__ENV.EXTRA_HEADERS) {
    Object.assign(headers, JSON.parse(__ENV.EXTRA_HEADERS));
  }
  if (__ENV.CONTENT_TYPE) {
    headers['Content-Type'] = __ENV.CONTENT_TYPE;
  }

  const response = http.request(
    method,
    `${baseUrl.replace(/\/$/, '')}${requestPath}`,
    __ENV.BODY || null,
    {
      headers,
      tags: {
        scenario: __ENV.SCENARIO || 'rate-limit',
      },
    },
  );

  if (response.status >= 200 && response.status < 300) {
    allowedResponses.add(1);
  } else if (response.status === 429) {
    rejectedResponses.add(1);
  } else if (response.status === 503) {
    unavailableResponses.add(1);
  } else {
    unexpectedResponses.add(1);
  }
}
