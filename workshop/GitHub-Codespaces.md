# GitHub Codespaces Setup

Run the workshop in your browser. No local install needed — everything (JDK, Maven, Docker, IDE) lives in a cloud container. Good for laptops without local Docker, conference workshops, or trying things out.

## What you'll need

- A GitHub account (free is fine)
- A browser

That's it.

## Launch a Codespace

1. Navigate to <https://github.com/jeremyrdavis/QuarkusInsights>
2. Click the green **Code** button → **Codespaces** tab → **Create codespace on main**
3. Wait 60–90 seconds while the container builds. You'll get a VS Code editor in your browser, with the repo opened in the workspace root.

## Get to the starter project

In the integrated terminal (View → Terminal, or `` Ctrl-` ``):

```bash
cd workshop/01-Episode-End-to-End/module-01-code
./mvnw test
```

The first run takes longer (Maven downloads, Docker pull) — expect 2–4 minutes. Subsequent runs are much faster.

You should see:

```
[INFO] Tests run: 25, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

If `HealthCheckSmokeTest` is green, you're ready. Open `workshop/01-Episode-End-to-End/01-Value-Objects.md` from the file explorer and start Step 1.

## Working in the browser IDE

Codespaces gives you a full VS Code in the browser with the Java extension pack pre-installed. You can:

- Edit files (Ctrl/Cmd-S to save — there's no auto-save by default in Codespaces)
- Run tests by right-clicking on a test class → **Run Tests**, or with the `./mvnw test` CLI
- Use the integrated terminal for everything `./mvnw` related
- Use the VS Code source-control panel to track your changes vs the starter

If you want to preview the running app, run `./mvnw quarkus:dev` in the terminal. Codespaces will forward port 8080; the **Ports** tab at the bottom shows the public URL. Click it to open the app in a new tab.

## Saving your work

Codespaces are ephemeral by default. If you want to keep your work after the session:

- **Commit and push to a fork** — the easiest path. Fork the repo first, push to your fork.
- **Download the diff** — `git diff > my-changes.patch` then download `my-changes.patch` via VS Code's file menu.

GitHub also auto-stops idle Codespaces after 30 minutes and deletes them after 30 days. Forks and patches survive both.

## Resource limits

Free GitHub plans get **60 hours/month** of Codespaces compute on the smallest VM (2 cores, 8 GB RAM). The workshop comfortably fits — each step takes 15–30 minutes, and `./mvnw test` runs in <30s. You can stop the Codespace between steps to save hours.

If `quarkus:dev` runs into memory pressure (heap errors during tests), bump to a 4-core machine in the Codespace settings. Costs a bit more hours but the dev experience is smoother.

## Troubleshooting

**Codespace fails to build**

Click **View Creation Log** at the bottom of the loading screen. Most common cause: Codespace prebuild image is out of date — try again in a few minutes, or click **Create Codespace** instead of using a prebuild.

**`docker: command not found` or Dev Services fails**

Codespaces should provide Docker out of the box, but occasionally the docker-in-docker feature times out. Restart the Codespace (Command Palette → **Codespaces: Stop Current Codespace**, then start it again).

**Tests run but `quarkus:dev` won't open in the browser**

Make sure the port forwarded by Codespaces is set to **Public**. The Ports tab shows visibility for each forwarded port; right-click → **Port Visibility** → **Public**.

## Alternative: local setup

If you'd rather work locally, see [Quarkus-Local.md](Quarkus-Local.md).
