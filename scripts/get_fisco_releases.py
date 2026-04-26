import urllib.request
import json

url = 'https://api.github.com/repos/FISCO-BCOS/FISCO-BCOS/releases/tags/v3.11.0'
headers = {'User-Agent': 'Mozilla/5.0'}

try:
    req = urllib.request.Request(url, headers=headers)
    with urllib.request.urlopen(req, timeout=30) as response:
        data = json.loads(response.read())
        print('Assets:')
        for asset in data.get('assets', []):
            print(f"  - {asset['name']}")
except Exception as e:
    print(f'Error: {e}')
