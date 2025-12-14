const express = require('express');
const cors = require('cors');

const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors());
app.use(express.json());

// In-memory storage for citizen status across worlds
// In production, you'd want to use Redis or a database
const citizenData = new Map();

// Cleanup stale data every 30 seconds
const STALE_THRESHOLD_MS = 60000; // 60 seconds
setInterval(() => {
    const now = Date.now();
    for (const [world, data] of citizenData.entries()) {
        if (now - data.lastUpdateTime > STALE_THRESHOLD_MS) {
            citizenData.delete(world);
            console.log(`Removed stale data for world ${world}`);
        }
    }
}, 30000);

// GET - Retrieve all citizen statuses
app.get('/api/citizens', (req, res) => {
    const statuses = Array.from(citizenData.values());
    res.json(statuses);
});

// GET - Retrieve status for a specific world
app.get('/api/citizens/:world', (req, res) => {
    const world = parseInt(req.params.world);
    const status = citizenData.get(world);
    
    if (status) {
        res.json(status);
    } else {
        res.status(404).json({ error: 'No data for this world' });
    }
});

// POST - Update citizen status for a world
app.post('/api/citizens', (req, res) => {
    const { playerName, world, distracted, timestamp } = req.body;
    
    if (world === undefined || distracted === undefined) {
        return res.status(400).json({ error: 'Missing required fields: world, distracted' });
    }
    
    const existingData = citizenData.get(world);
    const now = Date.now();
    
    // Track distraction timing for cycle prediction
    let distractionStartTime = 0;
    let distractionEndTime = existingData?.distractionEndTime || 0;
    
    if (distracted) {
        // Distraction starting
        distractionStartTime = existingData?.distracted ? existingData.distractionStartTime : now;
        // Keep previous end time for reference
    } else {
        // Distraction ending
        if (existingData?.distracted) {
            // Was distracted, now ending - record end time
            distractionEndTime = now;
        }
        // If wasn't distracted before, keep existing end time
    }
    
    const newData = {
        world: world,
        distracted: distracted,
        distractionStartTime: distractionStartTime,
        distractionEndTime: distractionEndTime,
        lastUpdateTime: timestamp || now,
        reportedBy: playerName || 'Unknown'
    };
    
    citizenData.set(world, newData);
    
    const secondsSinceEnd = distractionEndTime ? Math.floor((now - distractionEndTime) / 1000) : -1;
    const timeInfo = distracted ? 'DISTRACTED' : `ended ${secondsSinceEnd}s ago`;
    console.log(`World ${world}: ${timeInfo} (reported by ${playerName})`);
    
    res.json({ success: true, data: newData });
});

// DELETE - Remove status for a world (optional endpoint)
app.delete('/api/citizens/:world', (req, res) => {
    const world = parseInt(req.params.world);
    const deleted = citizenData.delete(world);
    
    if (deleted) {
        res.json({ success: true, message: `Removed data for world ${world}` });
    } else {
        res.status(404).json({ error: 'No data for this world' });
    }
});

// Health check endpoint
app.get('/health', (req, res) => {
    res.json({ 
        status: 'ok', 
        worldsTracked: citizenData.size,
        uptime: process.uptime()
    });
});

// Stats endpoint
app.get('/api/stats', (req, res) => {
    const statuses = Array.from(citizenData.values());
    const distractedCount = statuses.filter(s => s.distracted).length;
    
    res.json({
        totalWorlds: statuses.length,
        distractedWorlds: distractedCount,
        notDistractedWorlds: statuses.length - distractedCount
    });
});

app.listen(PORT, () => {
    console.log(`Citizen Cycle server running on port ${PORT}`);
    console.log(`API endpoints:`);
    console.log(`  GET  /api/citizens     - Get all citizen statuses`);
    console.log(`  GET  /api/citizens/:id - Get status for specific world`);
    console.log(`  POST /api/citizens     - Update citizen status`);
    console.log(`  GET  /api/stats        - Get statistics`);
    console.log(`  GET  /health           - Health check`);
});
