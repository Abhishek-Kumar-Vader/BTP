import os
import re
import urllib.request
import gzip
from tqdm import tqdm

OUTPUT_PATH = "data/raw/odia_corpus.txt"
MIN_SENTENCES = 50_000  # Minimum target for good accuracy

def clean_text(text):
    text = re.sub(r'http\S+|www\S+', '', text)
    text = re.sub(r'<[^>]+>', '', text)
    text = re.sub(r'[a-zA-Z]', '', text)
    text = re.sub(r'\s+', ' ', text).strip()
    return text

def is_valid_odia(text):
    if not text or len(text.strip()) < 5:
        return False
    odia_chars = sum(1 for c in text if '\u0B00' <= c <= '\u0B7F')
    return odia_chars / max(len(text), 1) > 0.2

def download_ai4bharat_odia():
    """
    Download from AI4Bharat's publicly available Odia monolingual corpus.
    This is hosted on their servers without login requirement.
    """
    print("Downloading AI4Bharat Odia corpus...")
    print("  URL: https://indicnlp.ai4bharat.org/resources/")
    
    # AI4Bharat monolingual corpus download links
    urls = [
        "https://storage.googleapis.com/ai4bharat-public-indic-nlp-corpora/data/monolingual/indicnlp/od.txt",
        "https://storage.googleapis.com/ai4bharat-public-indic-nlp-corpora/data/monolingual/ai4bharat/od.txt",
    ]
    
    all_sentences = []
    for url in urls:
        try:
            print(f"  Trying {url}...")
            req = urllib.request.Request(
                url,
                headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'}
            )
            with urllib.request.urlopen(req, timeout=30) as response:
                text = response.read().decode('utf-8', errors='ignore')
                lines = text.split('\n')
                for line in lines:
                    line = clean_text(line)
                    if is_valid_odia(line):
                        all_sentences.append(line)
                print(f"  ✓ Got {len(lines)} lines from this source")
        except Exception as e:
            print(f"  ✗ Failed: {e}")
    
    print(f"  Total collected: {len(all_sentences)} sentences")
    return all_sentences

def download_wikipedia_dump():
    """
    Alternative: Download Odia Wikipedia dump directly from Wikimedia.
    This is always available without login.
    """
    print("\nDownloading Odia Wikipedia dump...")
    print("  This is a large file (~50MB compressed), may take 5-10 minutes...")
    
    wiki_url = "https://dumps.wikimedia.org/orwiki/latest/orwiki-latest-pages-articles.xml.bz2"
    wiki_path = "data/raw/orwiki-latest-pages-articles.xml.bz2"
    
    try:
        # Download the dump
        req = urllib.request.Request(wiki_url, headers={'User-Agent': 'Mozilla/5.0'})
        with urllib.request.urlopen(req, timeout=120) as response:
            total_size = int(response.headers.get('Content-Length', 0))
            print(f"  File size: {total_size / (1024*1024):.1f} MB")
            
            with open(wiki_path, 'wb') as f:
                downloaded = 0
                chunk_size = 8192
                while True:
                    chunk = response.read(chunk_size)
                    if not chunk:
                        break
                    f.write(chunk)
                    downloaded += len(chunk)
                    if total_size > 0:
                        percent = (downloaded / total_size) * 100
                        print(f"\r  Downloading: {percent:.1f}%", end='')
        
        print(f"\n  Saved to {wiki_path}")
        
        # Extract text from XML (simplified extraction)
        print("  Extracting text from XML...")
        sentences = extract_wiki_text(wiki_path)
        return sentences
        
    except Exception as e:
        print(f"  ✗ Wikipedia download failed: {e}")
        return []

def extract_wiki_text(xml_path):
    """Simple extraction of text from Wikipedia XML dump."""
    import bz2
    sentences = []
    
    try:
        with bz2.open(xml_path, 'rt', encoding='utf-8', errors='ignore') as f:
            buffer = ""
            in_text = False
            for line in tqdm(f, desc="Parsing XML"):
                if '<text' in line:
                    in_text = True
                    buffer = ""
                elif '</text>' in line:
                    in_text = False
                    # Clean and split buffer
                    text = re.sub(r'<[^>]+>', ' ', buffer)  # Remove XML tags
                    text = re.sub(r'\{\{[^}]+\}\}', ' ', text)  # Remove templates
                    text = re.sub(r'\[\[([^]|]+)\|?[^]]*\]\]', r'\1', text)  # Clean wiki links
                    text = clean_text(text)
                    
                    for sent in text.split('।'):
                        sent = sent.strip()
                        if is_valid_odia(sent) and len(sent.split()) >= 3:
                            sentences.append(sent)
                    
                    if len(sentences) >= MIN_SENTENCES:
                        break
                        
                elif in_text:
                    buffer += line + " "
                    
    except Exception as e:
        print(f"  Error parsing XML: {e}")
    
    return sentences

def create_enhanced_sample_data():
    """Create larger sample data for testing pipeline."""
    print("\nCreating enhanced sample data for testing...")
    
    base_sentences = [
        "ଭାରତ ଏକ ବଡ଼ ଦେଶ ଅଟେ",
        "ଓଡ଼ିଆ ଭାଷା ବହୁତ ସୁନ୍ଦର",
        "ମୁଁ ଓଡ଼ିଆ କହେ",
        "ଭାରତର ରାଜଧାନୀ ଦିଲ୍ଲୀ ଅଟେ",
        "ଆମେ ସମସ୍ତେ ଭାରତୀୟ",
        "ଓଡ଼ିଆ ସାହିତ୍ୟ ବହୁତ ପୁରାତନ",
        "କଟକ ଏକ ସୁନ୍ଦର ସହର",
        "ଭୁବନେଶ୍ୱର ଓଡ଼ିଶାର ରାଜଧାନୀ",
        "ମୁଁ ବହୁତ ଖୁସି",
        "ଆଜି ଦିନଟି ଭଲ",
        "ସେ ଏକ ଭଲ ବ୍ୟକ୍ତି",
        "ପାଣି ପିଇବା ଦରକାର",
        "ବିଦ୍ୟାଳୟକୁ ଯିବା ଉଚିତ",
        "ପଢ଼ା ଲେଖା ବହୁତ ଗୁରୁତ୍ୱପୂର୍ଣ୍ଣ",
        "ଆମ ଦେଶ ବହୁତ ସୁନ୍ଦର",
        "ସମୁଦ୍ର କୂଳ ବହୁତ ସୁନ୍ଦର",
        "ମାଆ ବାପା ମୋର ପ୍ରେରଣା",
        "ବନ୍ଧୁ ମାନେ ବହୁତ ଭଲ",
        "ଖାଦ୍ୟ ବହୁତ ସୁଆଦିଆ",
        "ପୁସ୍ତକ ପଢ଼ିବା ଭଲ ଅଭ୍ୟାସ",
        "ଗାଁ ରେ ବହୁତ ଶାନ୍ତି",
        "ବର୍ଷା ଦିନେ ମାଟି ଗନ୍ଧ ବହୁତ ଭଲ",
        "ଫୁଲ ମାନେ ସୁନ୍ଦର",
        "ପଖି ମାନେ ଆକାଶରେ ଉଡନ୍ତି",
        "ନଦୀ ପାଣି ପିଇବା ପାଇଁ ସଫା",
        "ପର୍ବତ ମାନେ ଉଚା",
        "ଚାଷୀ ମାନେ କଷ୍ଟ କରନ୍ତି",
        "ଶିକ୍ଷକ ମାନେ ଜ୍ଞାନ ଦିଅନ୍ତି",
        "ଡାକ୍ତର ରୋଗୀ କୁ ସୁସ୍ଥ କରନ୍ତି",
        "ପୋଲିସ ଆମ ସୁରକ୍ଷା କରନ୍ତି",
        "ବାହାଘର ଏକ ଆନନ୍ଦ ର ଉତ୍ସବ",
        "ରଥଯାତ୍ରା ଓଡ଼ିଶାର ବଡ଼ ପର୍ବ",
        "ଦୁର୍ଗା ପୂଜା ବହୁତ ଧୁମଧାମ ରେ ହୁଏ",
        "ଲକ୍ଷ୍ମୀ ପୂଜା ଘରେ ଘରେ ହୁଏ",
        "କଣ୍ଟିଆ ପିଠା ବହୁତ ସୁଆଦିଆ",
        "ଦହି ବରା ଓଡ଼ିଆ ର ପ୍ରସିଦ୍ଧ ଖାଦ୍ୟ",
        "ଚେନା ପୋଡ଼ା ମିଠା ଲାଗେ",
        "ରସଗୋଲ୍ଲା ବହୁତ ଲୋକପ୍ରିୟ",
        "ସନ୍ଧ୍ୟା ବେଳେ ସୂର୍ଯ୍ୟାସ୍ତ ସୁନ୍ଦର",
        "ପ୍ରଭାତ ସମୟରେ ପକ୍ଷୀ ମାନେ କୁହୁକୁହୁ କରନ୍ତି",
        "ରାତି ଆକାଶ ରେ ତାରା ମାନେ ଚମକନ୍ତି",
        "ଚନ୍ଦ୍ର ପୂର୍ଣ୍ଣିମା ରେ ବହୁତ ସୁନ୍ଦର",
        "ବାତ୍ୟା ଆସିଲେ ବହୁତ କ୍ଷତି ହୁଏ",
        "ବନ୍ୟା ସମୟରେ ସାବଧାନ ରହିବା ଉଚିତ",
        "ଭୂମିକମ୍ପ ବେଳେ ଘର ଭିତରେ ରୁହ",
        "ଅଗ୍ନିକାଣ୍ଡ ବେଳେ ନିଆଁ ବୁଝାଇବା ଦରକାର",
    ]
    
    # Create variations by combining words differently
    extended = []
    for sent in base_sentences:
        words = sent.split()
        if len(words) >= 4:
            # Original
            extended.append(sent)
            # Swap some words
            if len(words) >= 5:
                extended.append(" ".join(words[:2] + [words[3], words[2]] + words[4:]))
    
    # Repeat to reach target
    while len(extended) < MIN_SENTENCES:
        extended.extend(base_sentences)
    
    return extended[:MIN_SENTENCES]

def main():
    os.makedirs("data/raw", exist_ok=True)
    
    print("="*60)
    print("ODIA CORPUS DOWNLOADER")
    print("="*60)
    
    all_sentences = []
    
    # Try AI4Bharat first (best quality, no login)
    ai4bharat = download_ai4bharat_odia()
    all_sentences.extend(ai4bharat)
    
    # If not enough, try Wikipedia
    if len(all_sentences) < MIN_SENTENCES:
        wiki = download_wikipedia_dump()
        all_sentences.extend(wiki)
    
    # If still not enough, use enhanced sample data
    if len(all_sentences) < MIN_SENTENCES:
        sample = create_enhanced_sample_data()
        all_sentences.extend(sample)
    
    # Deduplicate
    all_sentences = list(dict.fromkeys(all_sentences))
    
    # Save
    with open(OUTPUT_PATH, 'w', encoding='utf-8') as f:
        for s in all_sentences:
            f.write(s + '\n')
    
    print(f"\n{'='*60}")
    print(f"TOTAL SENTENCES SAVED: {len(all_sentences)}")
    print(f"   Location: {OUTPUT_PATH}")
    print(f"{'='*60}")
    
    if len(all_sentences) < MIN_SENTENCES:
        print(f"\nWARNING: Only {len(all_sentences)} sentences (target: {MIN_SENTENCES})")
        print("   For better accuracy, manually download Odia Wikipedia dump:")
        print("   https://dumps.wikimedia.org/orwiki/latest/")
        print("   Place the .xml.bz2 file in data/raw/ and re-run.")
    else:
        print("\nGreat! You have enough data for training.")

if __name__ == "__main__":
    main()