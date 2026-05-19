import os
import pickle
import math

MODELS_DIR = "models/spell_correction"
MAX_EDIT_DIST = 2
TOP_K = 5

# Odia Unicode characters
ODIA_CHARS = [chr(c) for c in range(0x0B00, 0x0B80)]

# Phonetic similarity groups (Odia-specific)
# Characters that sound similar and are often confused
PHONETIC_GROUPS = [
    {"ସ", "ଶ", "ଷ"},           # s/sh sounds (most commonly confused)
    {"ନ", "ଣ"},                 # na sounds
    {"ର", "ଡ଼", "ଢ଼"},          # ra/rra sounds
    {"ଲ", "ଳ"},                 # la sounds
    {"ଦ", "ଧ"},                 # da/dha
    {"ବ", "ଭ"},                 # ba/bha
    {"ପ", "ଫ"},                 # pa/pha
    {"କ", "ଖ"},                 # ka/kha
    {"ଗ", "ଘ"},                 # ga/gha
    {"ଚ", "ଛ"},                 # cha/chha
    {"ଜ", "ଝ"},                 # ja/jha
    {"ଟ", "ଠ"},                 # ta/tha (retroflex)
    {"ଡ", "ଢ"},                 # da/dha (retroflex)
    {"ତ", "ଥ"},                 # ta/tha (dental)
]


# Fat-finger keyboard physical proximity (Standard mobile layout groupings)
KEYBOARD_PROXIMITY_GROUPS = [
    {"କ", "ଖ", "ଗ", "ଘ"},       # Top row consonants
    {"ଚ", "ଛ", "ଜ", "ଝ"},       # Second row
    {"ଟ", "ଠ", "ଡ", "ଢ"},       # Retroflex row
    {"ତ", "ଥ", "ଦ", "ଧ", "ନ"},  # Dental row
    {"ପ", "ଫ", "ବ", "ଭ", "ମ"},  # Labial row
    {"ଯ", "ର", "ଲ", "ଳ", "ୱ"},  # Bottom row
    {"ା", "ି", "ୀ"},            # Common vowel matras next to each other
    {"ୁ", "ୂ", "ୃ"},
    {"େ", "ୈ", "ୋ", "ୌ"}
]

def levenshtein(s1, s2):
    """O(n) space Levenshtein with phonetic & keyboard proximity weighting."""
    if s1 == s2: return 0
    if not s1: return len(s2)
    if not s2: return len(s1)
    
    prev = list(range(len(s2) + 1))
    for i, c1 in enumerate(s1):
        curr = [i + 1]
        for j, c2 in enumerate(s2):
            cost = 0 if c1 == c2 else 1
            
            # Apply discounts for specific types of common Odia errors
            if cost == 1:
                # 1. Phonetic discount
                for group in PHONETIC_GROUPS:
                    if c1 in group and c2 in group:
                        cost = 0.3  
                        break
                
                # 2. Keyboard proximity discount (if not already phonetically matched)
                if cost == 1:
                    for k_group in KEYBOARD_PROXIMITY_GROUPS:
                        if c1 in k_group and c2 in k_group:
                            cost = 0.5  
                            break
            
            curr.append(min(curr[j] + 1, prev[j + 1] + 1, prev[j] + cost))
        prev = curr
    return prev[-1]

def edits1(word):
    splits = [(word[:i], word[i:]) for i in range(len(word) + 1)]
    deletes = [L + R[1:] for L, R in splits if R]
    transposes = [L + R[1] + R[0] + R[2:] for L, R in splits if len(R) > 1]
    replaces = [L + c + R[1:] for L, R in splits if R for c in ODIA_CHARS]
    inserts = [L + c + R for L, R in splits for c in ODIA_CHARS]
    return set(deletes + transposes + replaces + inserts)

def edits2(word):
    return set(e2 for e1 in edits1(word) for e2 in edits1(e1))


class OdiaSpellCorrector:
    def __init__(self):
        with open(os.path.join(MODELS_DIR, "ngram_model.pkl"), 'rb') as f:
            data = pickle.load(f)
        self.unigrams = data["unigrams"]
        self.bigrams = data["bigrams"]
        self.dictionary = set(data["valid_words"].keys())
        self.total = data["total"]
        self.V = len(self.unigrams)
        print(f"Loaded {len(self.dictionary)} words")

    def unigram_prob(self, word):
        """Smoothed unigram probability."""
        return (self.unigrams.get(word, 0) + 0.1) / (self.total + self.V * 0.1)

    def bigram_prob(self, prev, word):
        """Bigram with backoff."""
        if not prev or prev not in self.bigrams:
            return self.unigram_prob(word)
        prev_count = sum(self.bigrams[prev].values())
        if prev_count == 0:
            return self.unigram_prob(word)
        return (self.bigrams[prev].get(word, 0) + 0.1) / (prev_count + self.V * 0.1)

    def score_candidate(self, candidate, original, prev=None):
        # 1. N-gram score
        if prev and prev in self.dictionary:
            ngram_score = self.bigram_prob(prev, candidate)
        else:
            ngram_score = self.unigram_prob(candidate)
        
        ngram_score = min(1.0, ngram_score * 100) 

        # 2. Edit distance score
        edit_dist = levenshtein(original, candidate)
        max_len = max(len(original), len(candidate))
        if max_len > 0:
            edit_score = math.exp(-2.0 * edit_dist / max_len) 
        else:
            edit_score = 1.0

        # 3. Phonetic & Proximity bonus
        bonus_score = 0.0
        min_len = min(len(original), len(candidate))
        if min_len > 0:
            matches = 0
            for i in range(min_len):
                c1, c2 = original[i], candidate[i]
                if c1 == c2:
                    matches += 1
                else:
                    for p_group in PHONETIC_GROUPS:
                        if c1 in p_group and c2 in p_group:
                            matches += 0.7
                            break
                    for k_group in KEYBOARD_PROXIMITY_GROUPS:
                        if c1 in k_group and c2 in k_group:
                            matches += 0.5
                            break
            bonus_score = matches / max_len

        # 4. NEW: Autocomplete & Truncation Bonus
        prefix_bonus = 0.0
        if candidate.startswith(original) and len(original) >= 2:
            prefix_bonus = 0.3  # Bonus if it's an auto-completion (ଭାର -> ଭାରତ)
        elif original.startswith(candidate) and len(candidate) >= 2:
            prefix_bonus = 0.4  # Bonus if we removed trailing junk (ଭାରତ୍ତ୍ତ -> ଭାରତ)

        # Combine scores
        combined = (0.50 * ngram_score + 
                    0.20 * edit_score + 
                    0.15 * bonus_score +
                    0.15 * prefix_bonus)
        
        return combined

    def correct(self, word, prev=None, top_k=5):
        """Get top-K corrections with Autocomplete and Junk Removal."""
        candidates = set()

        # 1. NEW: Truncation check (Fixes trailing junk like ଭାରତ୍ତ୍ତ -> ଭାରତ)
        truncations = {word[:i] for i in range(len(word), max(0, len(word)-6), -1)} & self.dictionary
        candidates.update(truncations)

        # 2. Standard Edits (Fixes normal typos)
        edits = edits1(word) & self.dictionary
        if not edits and MAX_EDIT_DIST >= 2:
            edits = edits2(word) & self.dictionary
        candidates.update(edits)

        # 3. NEW: Autocomplete / Prefix (Fixes short words like ଭାର -> ଭାରତ)
        if word in self.dictionary or len(word) >= 2:
            completions = [w for w in self.dictionary if w.startswith(word) and w != word]
            # Sort by frequency and take top 3 to avoid flooding candidates
            completions.sort(key=lambda x: self.unigrams.get(x, 0), reverse=True)
            candidates.update(completions[:3])

        # 4. Always include the word itself if it's in the dictionary
        if word in self.dictionary:
            candidates.add(word)

        if not candidates:
            return [(word, 0.0)]

        # Score all candidates
        scored = []
        for c in candidates:
            score = self.score_candidate(c, word, prev)
            scored.append((c, score))

        # Sort by highest score
        scored.sort(key=lambda x: -x[1])
        
        # Normalize top score to 1.0
        if scored:
            max_score = scored[0][1]
            if max_score > 0:
                scored = [(w, s/max_score) for w, s in scored]
        
        return scored[:top_k]
    
    def correct_sentence(self, sentence):
        """Splits a full sentence, corrects word-by-word, and uses rolling context."""
        words = sentence.split()
        corrected_words = []
        prev_word = None

        for word in words:
            # Get the top 1 prediction for the current word
            suggestions = self.correct(word, prev=prev_word, top_k=1)
            best_correction = suggestions[0][0]
            
            corrected_words.append(best_correction)
            # Update the context so the next word uses this corrected word
            prev_word = best_correction 

        return " ".join(corrected_words)

def main():
    print("="*60)
    print(" ODIA SPELL CHECKER")
    print("    Type 'exit' to quit the program")
    print("="*60)
    
    print("\nLoading models... (This might take a few seconds)")
    corrector = OdiaSpellCorrector()
    print("Ready!\n")
    
    while True:
        try:
            user_input = input("\nInput : ").strip()
            
            if user_input.lower() in ['exit', 'quit']:
                print("Shutting down...")
                break
                
            if not user_input:
                continue
                
            # Count how many words the user typed
            words = user_input.split()
            
            if len(words) == 1:
                # SINGLE WORD: Show the top 5 math scores
                print("Output:")
                suggestions = corrector.correct(user_input, top_k=5)
                for i, (w, s) in enumerate(suggestions, 1):
                    print(f"   {i}. {w} (score: {s:.3f})")
            else:
                # FULL SENTENCE: Show the final auto-corrected string
                corrected = corrector.correct_sentence(user_input)
                print(f"Output: {corrected}")
                
        except KeyboardInterrupt:
            print("\nShutting down...")
            break

if __name__ == "__main__":
    main()