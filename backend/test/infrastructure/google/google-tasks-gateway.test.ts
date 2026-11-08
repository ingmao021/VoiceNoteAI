import { describe, expect, it } from 'vitest';
import { isAccessRevoked, isNotFound } from '../../../src/infrastructure/google/google-tasks-gateway.js';

describe('isAccessRevoked', () => {
  it('detects invalid_grant in the response or in the message', () => {
    expect(isAccessRevoked({ response: { data: { error: 'invalid_grant' } } })).toBe(true);
    expect(isAccessRevoked(new Error('invalid_grant: Token has been expired or revoked.'))).toBe(true);
    expect(isAccessRevoked(new Error('Request failed with status code 500'))).toBe(false);
  });
});

describe('isNotFound', () => {
  it('detects a 404 in either the code or the response status', () => {
    expect(isNotFound({ code: 404 })).toBe(true);
    expect(isNotFound({ response: { status: 404 } })).toBe(true);
    expect(isNotFound({ code: 500, response: { status: 500 } })).toBe(false);
  });
});
