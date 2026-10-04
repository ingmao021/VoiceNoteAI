import { createHash, timingSafeEqual } from 'node:crypto';
import type { FastifyReply, FastifyRequest } from 'fastify';

const BEARER_PREFIX = 'Bearer ';

/** Hook `onRequest` que exige `Authorization: Bearer <token>` con el token de la app. */
export function bearerAuth(expectedToken: string) {
  const expectedDigest = digest(expectedToken);
  return async (req: FastifyRequest, reply: FastifyReply) => {
    if (isAuthorized(req.headers.authorization, expectedDigest)) return;
    return reply
      .code(401)
      .header('www-authenticate', 'Bearer')
      .send({ error: 'invalid_token', message: 'Missing or invalid bearer token' });
  };
}

function isAuthorized(header: string | undefined, expectedDigest: Buffer): boolean {
  if (!header?.startsWith(BEARER_PREFIX)) return false;
  // Comparar huellas de igual longitud evita filtrar la longitud del token por tiempo.
  return timingSafeEqual(digest(header.slice(BEARER_PREFIX.length)), expectedDigest);
}

function digest(value: string): Buffer {
  return createHash('sha256').update(value).digest();
}
