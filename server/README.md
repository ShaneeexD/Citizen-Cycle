# Citizen Cycle Server

A simple backend server for crowdsourcing Wealthy Citizen distraction data.

## Local Setup

1. Install Node.js (v18 or higher)
2. Install dependencies:
   ```bash
   npm install
   ```
3. Start the server:
   ```bash
   npm start
   ```

The server will run on `http://localhost:3000` by default.

## Deploy to Railway (Recommended)

1. Push the `server` folder to a GitHub repository
2. Go to [railway.app](https://railway.app) and sign in with GitHub
3. Click "New Project" → "Deploy from GitHub repo"
4. Select your repository and the `server` folder
5. Railway will auto-detect Node.js and deploy
6. Get your public URL from the deployment settings

## Deploy to Render

1. Push to GitHub
2. Go to [render.com](https://render.com) and create a new "Web Service"
3. Connect your GitHub repo
4. Set:
   - Build Command: `npm install`
   - Start Command: `npm start`
5. Deploy and get your public URL

## Deploy to Fly.io

1. Install flyctl: `curl -L https://fly.io/install.sh | sh`
2. Run: `fly launch` in the server folder
3. Deploy: `fly deploy`

## API Endpoints

### GET /api/citizens
Returns all tracked citizen statuses.

**Response:**
```json
[
  {
    "world": 301,
    "distracted": true,
    "distractionStartTime": 1702567890000,
    "lastUpdateTime": 1702567895000,
    "reportedBy": "PlayerName"
  }
]
```

### GET /api/citizens/:world
Returns status for a specific world.

### POST /api/citizens
Update citizen status for a world.

**Request Body:**
```json
{
  "playerName": "YourName",
  "world": 301,
  "distracted": true,
  "timestamp": 1702567890000
}
```

### GET /api/stats
Returns statistics about tracked worlds.

### GET /health
Health check endpoint.

## Configuration in RuneLite Plugin

In the Citizen Cycle plugin settings:
- **API Endpoint**: `http://localhost:3000/api/citizens`
- **Authorization**: (leave empty for local testing)

## Deployment

For production use, consider:
- Using a proper database (Redis, MongoDB, PostgreSQL)
- Adding authentication
- Deploying to a cloud service (Heroku, Railway, Render, etc.)
- Adding rate limiting
- Using HTTPS

## Environment Variables

- `PORT` - Server port (default: 3000)
