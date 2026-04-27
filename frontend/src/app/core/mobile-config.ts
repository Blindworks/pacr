/**
 * Backend-URL für Capacitor (iOS/Android).
 *
 * Im Web-Browser wird diese Datei nicht verwendet – dort bleibt die App
 * bei relativen URLs ('/api'), die vom Nginx (prod) bzw. Angular dev-server (dev)
 * an das Backend weitergeleitet werden.
 *
 * Auf Native (iOS/Android) lebt die App unter capacitor://localhost und braucht
 * eine absolute URL, weil es keinen Proxy gibt.
 *
 * Zum Umstellen einfach die Konstante MOBILE_API_BASE_URL ändern und neu builden:
 *   npm run build && npx cap sync ios
 *
 * Beispiele:
 *   - Production:           'https://pacr.app/api'
 *   - Lokales Backend:      'http://192.168.x.x:8080/api'  (Mac-IP des Dev-Rechners)
 *   - Staging-Server:       'https://staging.pacr.app/api'
 *
 * WICHTIG für lokales Backend-Testen:
 *   - 'localhost' funktioniert nicht, weil es im Simulator den Simulator selbst meint.
 *   - Auf echtem iPhone: Mac und iPhone müssen im selben WLAN sein.
 *   - Backend muss auf 0.0.0.0 lauschen (nicht nur 127.0.0.1).
 *   - iOS blockt http:// per Default. Workaround siehe Info.plist
 *     (NSAppTransportSecurity → NSAllowsArbitraryLoads für Dev).
 */
export const mobileApiBaseUrl = 'https://pacr.app/api';
