# Outlook Mail App

A Spring Boot web application that connects to Outlook / Microsoft 365 mailboxes
via the **Microsoft Graph API**. Users sign in interactively with their own
Microsoft account (OAuth2 authorization code flow), then can view their **Inbox**
and **send email**.

## Features

- "Sign in with Microsoft" — interactive browser login (Azure AD / Entra ID)
- Inbox view — last 25 messages, sender, subject, preview, unread highlighting
- Message detail view — full HTML body
- Compose screen — send mail as the signed-in user via `POST /me/sendMail`

## Tech stack

- Java 17, Spring Boot 3.3 (Maven)
- Spring Security + `spring-boot-starter-oauth2-client` for the sign-in flow
- Spring's `RestClient` for calling the Graph REST API directly (no extra SDK)
- Thymeleaf for server-rendered pages

## 1. Register an app in Azure AD (Entra ID)

1. Go to [portal.azure.com](https://portal.azure.com) → **Microsoft Entra ID** →
   **App registrations** → **New registration**.
2. Name it (e.g. "Outlook Mail App").
3. Under **Supported account types**, pick what fits your users
   (single tenant, or "Accounts in any organizational directory and personal
   Microsoft accounts").
4. Under **Redirect URI**, choose **Web** and enter:
   ```
   http://localhost:8080/login/oauth2/code/azure
   ```
5. After creation, note the **Application (client) ID** and **Directory
   (tenant) ID** from the Overview page.
6. Go to **Certificates & secrets** → **New client secret**, and copy the
   secret **value** (shown only once).
7. Go to **API permissions** → **Add a permission** → **Microsoft Graph** →
   **Delegated permissions**, and add:
   - `User.Read`
   - `Mail.Read`
   - `Mail.Send`
   - `offline_access` (needed for refresh tokens; usually pre-granted)

   Click **Grant admin consent** if your tenant requires it.

## 2. Configure the app

Set these as environment variables before running (do not hard-code secrets
into `application.yml`):

```bash
export AZURE_CLIENT_ID=your-application-client-id
export AZURE_CLIENT_SECRET=your-client-secret-value
export AZURE_TENANT_ID=your-directory-tenant-id   # or "common" for multi-tenant/personal accounts
```

## 3. Run it

```bash
mvn spring-boot:run
```

Then open [http://localhost:8080](http://localhost:8080), click **Sign in
with Microsoft**, and authenticate. You'll be redirected to `/inbox`.

## Project layout

```
src/main/java/com/example/outlookmail/
├── OutlookMailApplication.java     # entry point
├── config/SecurityConfig.java      # OAuth2 login + route protection
├── controller/MailController.java  # /inbox, /message/{id}, /send routes
├── service/GraphService.java       # calls Microsoft Graph REST API
└── dto/
    ├── MailMessage.java
    └── SendMailRequest.java
src/main/resources/
├── application.yml                 # Azure AD registration + Graph base URL
├── templates/                      # Thymeleaf pages (index, inbox, message, send)
└── static/css/style.css
```

## How authentication works

- Spring Security's `oauth2Login()` handles the full redirect dance with
  Azure AD's `/authorize` and `/token` endpoints (configured under
  `spring.security.oauth2.client.registration.azure` in `application.yml`).
- After login, Spring stores an `OAuth2AuthorizedClient` containing the
  access token (and refresh token) for the `azure` registration.
- Controllers grab the token via `@RegisteredOAuth2AuthorizedClient("azure")`
  and pass it as a `Bearer` header when calling Graph endpoints
  (`GET /me/mailFolders/inbox/messages`, `GET /me/messages/{id}`,
  `POST /me/sendMail`).

## Notes / next steps

- Access tokens are short-lived; Spring Security's default authorized-client
  manager will use the stored refresh token to renew them automatically on
  the next request as long as `offline_access` was granted.
- Only the Inbox folder and top 25 messages are fetched — extend
  `GraphService` with paging (`@odata.nextLink`) or other folders as needed.
- For production, move the client secret to a secrets manager, enable HTTPS,
  and register a proper HTTPS redirect URI in Azure AD.
