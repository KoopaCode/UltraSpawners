// Simulate Bukkit getDescription().getVersion()
const pluginDescription = {
  name: "UltraSpawners",
  version: "1.2.3" // change to whatever is in plugin.yml
};

function displayBanner() {
  const version = pluginDescription.version;
  const paddedVersion = version.padEnd(20, " ");

  console.log("╔═════════════════════════════════════════════════════════════════════════╗");
  console.log("║                                                                         ║");
  console.log("║     ██╗   ██╗██╗  ████████╗██████╗  █████╗                              ║");
  console.log("║     ██║   ██║██║  ╚══██╔══╝██╔══██╗██╔══██╗                             ║");
  console.log("║     ██║   ██║██║     ██║   ██████╔╝███████║                             ║");
  console.log("║     ██║   ██║██║     ██║   ██╔══██╗██╔══██║                             ║");
  console.log("║     ╚██████╔╝███████╗██║   ██║  ██║██║  ██║                             ║");
  console.log("║      ╚═════╝ ╚══════╝╚═╝   ╚═╝  ╚═╝╚═╝  ╚═╝                             ║");
  console.log("║                                                                         ║");
  console.log("║        ███████╗██████╗  █████╗ ██╗    ██╗███╗   ██╗███████╗██████╗      ║");
  console.log("║        ██╔════╝██╔══██╗██╔══██╗██║    ██║████╗  ██║██╔════╝██╔══██╗     ║");
  console.log("║        ███████╗██████╔╝███████║██║ █╗ ██║██╔██╗ ██║█████╗  ██████╔╝     ║");
  console.log("║        ╚════██║██╔═══╝ ██╔══██║██║███╗██║██║╚██╗██║██╔══╝  ██╔══██╗     ║");
  console.log("║        ███████║██║     ██║  ██║╚███╔███╔╝██║ ╚████║███████╗██║  ██║     ║");
  console.log("║        ╚══════╝╚═╝     ╚═╝  ╚═╝ ╚══╝╚══╝ ╚═╝  ╚═══╝╚══════╝╚═╝  ╚═╝     ║");
  console.log("║                                                                         ║");
  console.log("║   ═══════════════════════════════════════════════════════════════════   ║");
  console.log("║                                                                         ║");
  console.log("║                 Advanced Spawner Management System                      ║");
  console.log("║                                                                         ║");
  console.log(`║                     Version: ${paddedVersion}                       ║`);
  console.log("║                     Author: Koopa                                       ║");
  console.log("║                     Modrinth: modrinth.com/plugin/spawners         ║");
  console.log("║                                                                         ║");
  console.log("║   ═══════════════════════════════════════════════════════════════════   ║");
  console.log("║                                                                         ║");
  console.log("╚═════════════════════════════════════════════════════════════════════════╝");
}

displayBanner();
