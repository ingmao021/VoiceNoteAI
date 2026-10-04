import { describe, expect, it } from 'vitest';
import { isAccessRevoked } from '../../../src/infrastructure/google/google-tasks-gateway.js';

describe('isAccessRevoked', () => {
  it('detects invalid_grant in the response or in the message', () => {
    expect(isAccessRevoked({ response: { data: { error: 'invalid_grant' } } })).toBe(true);
    expect(isAccessRevoked(new Error('invalid_grant: Token has been expired or revoked.'))).toBe(true);
    expect(isAccessRevoked(new Error('Request failed with status code 500'))).toBe(false);
  });
});
