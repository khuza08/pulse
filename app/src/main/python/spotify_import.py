"""Spotify playlist import helper — called from Kotlin via Chaquopy."""
import json
import traceback

def get_playlist(url: str) -> str:
    """Fetch Spotify playlist tracks. Returns JSON string with name, thumbnail, tracks."""
    from spotify_scraper import SpotifyClient
    with SpotifyClient() as client:
        playlist = client.get_playlist(url, max_tracks=500)
        # use to_dict for everything — avoids attribute guessing
        pd = playlist.to_dict() if hasattr(playlist, 'to_dict') else {}
        tracks_raw = pd.get('tracks', None)
        if not tracks_raw:
            tracks_raw = list(playlist.tracks)

        tracks = []
        for item in tracks_raw:
            try:
                # each item is a PlaylistTrack dict with 'track' key
                if isinstance(item, dict):
                    td = item.get('track', item)
                elif hasattr(item, 'to_dict'):
                    td = item.to_dict().get('track', item.to_dict())
                else:
                    td = {}

                artists = td.get('artists', [])
                artist_names = [a.get('name', '') if isinstance(a, dict) else str(a) for a in artists]
                album = td.get('album', {}) if isinstance(td.get('album'), dict) else {}
                images = album.get('images', [])
                thumb = images[0].get('url', '') if images and isinstance(images[0], dict) else None

                tracks.append({
                    "title": td.get('name', ''),
                    "artists": artist_names,
                    "duration_ms": td.get('duration_ms', 0),
                    "thumbnail": thumb,
                })
            except Exception as e:
                print(f"Skipping track: {e}")
                continue

        # fall back to first track's art
        images = pd.get('images', []) if isinstance(pd.get('images'), list) else []
        thumb = images[0].get('url', '') if images and isinstance(images[0], dict) else None
        if not thumb and tracks:
            thumb = tracks[0].get('thumbnail')

        result = {
            "name": pd.get('name', playlist.name if hasattr(playlist, 'name') else "Spotify Playlist"),
            "thumbnail": thumb,
            "tracks": tracks,
        }
        return json.dumps(result)
