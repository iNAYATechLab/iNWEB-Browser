#pragma once
namespace network::mojom {
// Mirrors services/network/public/mojom/fetch_api.mojom @154.0.8037.21.
enum class RequestDestination {
  kEmpty = 0, kAudio = 1, kAudioWorklet = 2, kDocument = 3, kEmbed = 4,
  kFont = 5, kFrame = 6, kIframe = 7, kImage = 8, kManifest = 9,
  kObject = 10, kPaintWorklet = 11, kReport = 12, kScript = 13,
  kServiceWorker = 14, kSharedWorker = 15, kStyle = 16, kTrack = 17,
  kVideo = 18, kWebBundle = 19, kWorker = 20, kXslt = 21,
  kFencedframe = 22, kWebIdentity = 23, kCompressionDictionary = 24,
  kSpeculationRules = 25, kJson = 26, kEmailVerification = 28, kText = 29,
};
}  // namespace network::mojom
