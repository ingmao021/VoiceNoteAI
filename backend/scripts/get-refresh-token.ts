/**
 * Obtiene TU refresh token de Google una sola vez — andamio de la etapa 2 (PRD §12).
 * En la etapa 4 lo sustituye la vinculación por código (§3.4), que usa este mismo callback.
 *
 * Uso: con GOOGLE_CLIENT_ID y GOOGLE_CLIENT_SECRET en .env y el backend APAGADO (usa su puerto):
 *   npm run refresh-token
 * Requisito: en el cliente OAuth, URI de redirección autorizada = http://localhost:3000/auth/google/callback
 */
import { createServer } from 'node:http';
import { randomBytes } from 'node:crypto';
import { auth } from '@googleapis/tasks';

const PORT = 3000;
const CALLBACK_PATH = '/auth/google/callback';
const REDIRECT_URI = `http://localhost:${PORT}${CALLBACK_PATH}`;
const SCOPES = ['openid', 'email', 'https://www.googleapis.com/auth/tasks'];

const clientId = process.env.GOOGLE_CLIENT_ID;
const clientSecret = process.env.GOOGLE_CLIENT_SECRET;
if (!clientId || !clientSecret) {
  console.error('GOOGLE_CLIENT_ID or GOOGLE_CLIENT_SECRET missing in backend/.env');
  process.exit(1);
}

const oauth = new auth.OAuth2(clientId, clientSecret, REDIRECT_URI);
const state = randomBytes(16).toString('hex');
const authUrl = oauth.generateAuthUrl({
  scope: SCOPES,
  // offline + consent es lo que garantiza que Google devuelva refresh token (PRD §3.4).
  access_type: 'offline',
  prompt: 'consent',
  state,
});

const server = createServer(async (req, res) => {
  const request = new URL(req.url ?? '/', REDIRECT_URI);
  if (request.pathname !== CALLBACK_PATH) {
    res.writeHead(404).end();
    return;
  }
  const respond = (status: number, text: string) => {
    res.writeHead(status, { 'Content-Type': 'text/plain; charset=utf-8' }).end(text);
    server.close();
  };

  if (request.searchParams.get('state') !== state) return respond(400, 'State mismatch. Run the script again.');
  const error = request.searchParams.get('error');
  if (error) return respond(400, `Google returned an error: ${error}`);

  try {
    const { tokens } = await oauth.getToken(request.searchParams.get('code') ?? '');
    if (!tokens.refresh_token) {
      console.error('\nGoogle returned no refresh token. Revoke access at https://myaccount.google.com/permissions and retry.');
      return respond(502, 'No refresh token: check the terminal.');
    }
    console.log('\nDone. Add this line to backend/.env:\n');
    console.log(`GOOGLE_REFRESH_TOKEN=${tokens.refresh_token}\n`);
    respond(200, 'Done. Go back to the terminal; you can close this tab.');
  } catch (e) {
    console.error('\nCode exchange failed:', e instanceof Error ? e.message : e);
    respond(502, 'Code exchange failed: check the terminal.');
  }
});

server.listen(PORT, () => {
  console.log('Open this URL in the browser, sign in with your account and grant access to your tasks:\n');
  console.log(authUrl);
});
