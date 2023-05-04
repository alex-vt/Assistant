# Assistant

A quick access popup window for advanced operations on text.

* Powered by online language models **by OpenAI** - bring **your token**.
* **Type**, edit, **screen capture** or **dictate** your text.
* **Answer**, **summarize**, **rewrite**, **continue** text. Or make a **web search** out of it.
* **Big text** - no problem: will be **shortened** before the action is performed.
* **Choose language models** easily & **preview compute costs** every time.
* **Opens by shortcut** key press - use in any context, minimal distraction.

![Example](docs/example1.png)

#### Usage:

* App stays in background in tray
* `Meta`+`Space`: **open/close** assistant window
* `F1`..`F7`: **select action** on text, click action: run it
* `Ctrl`+`Enter`: **perform** selected **action** on text and put the result below
* `Ctrl`+`PrintScreen`, or screen capture button: select screen area to **capture text** from

Window height adjusts when text is short.

Estimated cost of action updates below continuously. Actual cost shows after action is performed.

Prefer a more capable language model? Use `Shift` - see the reference below.

## Usage reference

### Buttons

| Action \ Button | Press                                              |
|-----------------|----------------------------------------------------|
| Action buttons  | Select action to do with text in input form        |
| Clear           | Delete text from input form                        |
| Mic             | Use microphone to dictate text (see section below) |
| Screenshot      | Pick text from screen (see section below)          |

For usage with keyboard shortcuts, see Usage summary with modifier keys below.

### Choosing alternative language models

Instead of `Ctrl`+`Enter` to perform selected action on text,
key combos with other modifier keys can be used, when a different language model is preferable.

The default model, for `Ctrl` modifier key, is Turbo.
Other models are slower and more costly, but more capable.
_As a rule of thumb, the "longer" modifier key(s) pressed,
the more capable language model used._

The table below compares the language models - best values for each marked in bold.

#### Language model selection for modifier keys:

| Model properties \ Modifier keys                            | Ctrl            | Shift              | Ctrl+Shift |
|-------------------------------------------------------------|-----------------|--------------------|------------|
| Short name                                                  | Turbo           | DaVinci            | GPT4       |
| OpenAI model name                                           | `gpt-3.5-turbo` | `text-davinci-003` | `gpt-4`    |
| Capability                                                  | Worse           | Regular            | **Better** |
| Speed                                                       | **Faster**      | Regular            | Slower     |
| Cost estimate, USD for 1 page of text`*`                    | **0.0031**      | 0.031              | 0.093      |
| Pages of text fitting in 1 computing round, estimate`**`    | 3.5             | 3.5                | **7**      |
| Max cost, USD for 1 computing round, filled with pages`***` | **0.0082**      | 0.082              | 0.28       |

Costs of 1 page of input text consumption and 1 page of output text production by the model can be different - 
see per-unit specification in the table below.
The ratio of input to output may vary.
Assistant reserves 12.5% of max compute units capacity in each round for the output,
but the model can produce a smaller output than that. Up to remaining 87.5% of max compute units capacity can be taken by the input.

> `* A page is estimated at 2000 characters, with 1 compute unit being 2 characters. Compute unit (a token in OpenAI models) may be from around 4 letters in English plain text, around 1 non-letter character, and down to less than 1 character in some languages. Output is estimated at 12.5% of max compute units capacity for the used model, as reserved by Assistant. Input and output costs are added together. See per-unit specification in the table below.`

> `** A round is 1 call to OpenAI services (API). For a round at capacity, the input text is 87.5% of whole text. For input text not fitting into 1 round, see Big text processing info below. For a page estimate, see * marked note above. See per-unit specification in the table below.`

> `*** Max cost estimate of 1 OpenAI API call Assistant can make - at 100% of compute units capacity: with the cost being a proportion of 87.5% input page cost and 12.5% output page cost. For page/round estimates, see */** marked notes above. See per-unit specification in the table below.`

<details>
<summary>Per-unit specification of language models (click to view)</summary>

| Model specifications \ Model names                           | Turbo           | DaVinci            | GPT4    |
|--------------------------------------------------------------|-----------------|--------------------|---------|
| OpenAI language model name                                   | `gpt-3.5-turbo` | `text-davinci-003` | `gpt-4` |
| Cost, USD for 1 compute unit (1 token) of **input** text`*`  | 0.000002        | 0.00002            | 0.00003 |
| Cost, USD for 1 compute unit (1 token) of **output** text`*` | 0.000002        | 0.00002            | 0.00006 |
| Max compute units in 1 round (tokens in 1 API call)`**`      | 4096            | 4096               | 8192    |

> `* Assistant sends input text to the language model, the model produces and adds output text to it. Primary source of cost data: OpenAI.`

> `** Total size for input and output texts combined.`

</details>

#### Usage summary with modifier keys:

| Action \ Modifier keys           | (no modifier key)          | Ctrl                               | Shift                                | Ctrl+Shift                        |
|----------------------------------|----------------------------|------------------------------------|--------------------------------------|-----------------------------------|
| Hold the modifier keys           | Estimate cost for Turbo`*` | Estimate cost for Turbo            | Estimate cost for DaVinci            | Estimate cost for GPT4            |
| Press modifier keys + `Enter`    | Put new line in text       | Perform selected action with Turbo | Perform selected action with DaVinci | Perform selected action with GPT4 |                        |
| Press modifier keys + `F1`..`F7` | Select action              | Select & perform action with Turbo | Select & perform action with DaVinci | Select & perform action with GPT4 |

> `* Cost is estimated for Turbo model continuously without any keys pressed for convenience. Pressing and holding modifier keys for other models will adjust the estimate accordingly.`

### Big text processing & cost estimates

As language models have limits for how many compute units worth of input and output text combined
they can process in 1 round (API call), texts exceeding these limits are first split in parts, then
parts are shortened,
and finally the selected action is performed on the joined shortened parts.

#### Considerations & details regarding the big text splitting technique:

* The input text for consumption by a language model is text from the input form and Assistant's
  instructions for the selected action, joined together.
* Assistant reserves 12.5% of max compute units capacity of a round for output.
  Therefore, the criterion of input text being big is, its size in compute units exceeding 87.5% of
  max compute units capacity of a round.
* If input text isn't big, it's processed in (and cost estimated for) 1 round.
* As compute units for OpenAI models are tokens, input text size is measured in compute units by
  tokenization.
* Quality of text processing is negatively affected by context loss at boundaries between parts it's
  cut into, so less parts with max tokens in each is preferable.
* Part token count maximizing is computationally expensive if every substring of input text is
  tokenized.
  An alternative is doing binary search-like selection of substrings, with a tolerance value (10 is
  used).
* The parts are intended to be shortened, so the shortening command will be joined to each, as is
  factored in when splitting the big text.
* A part of big text is made by cutting off the starting substring from its remainder. The part size
  is fitted by tokenization to be as big as can fit into input text token size limit (or up to 10
  characters shorter).
* To reduce loss of context at big text cut points, the parts overlap: after a part is cut off,
  the remainder of big text still keeps the last 100 characters from that part.
* Overlap isn't needed after the last part.
* The resulting parts are shortened, each joined with the shortening command, each in 1 round.
* If the joined shortened parts are still a text too big for 1 round, splitting is done again.
* Once the joined shortened parts can be processed is 1 round with the selected action, it is
  finally done.
* Shortening is done always by Turbo model due to faster speed and lower cost.
* Total cost is calculated as sum for all Turbo-shortening rounds and the final action round done
  with the selected model.

#### Cost estimates for example text sizes:

For all input text sizes, including those small enough to be processed in 1 round, the text output
is estimated at its max size of 12.5% of model's round compute units limit, as reserved by
Assistant.

For 1-round processing estimation info, see language model info tables above. Definitions of a page
and a round are the same here as there:

* 1 compute unit = 1 OpenAI token = 2 characters
* 1 page = 2000 characters = 1000 compute units = 1000 tokens
* 1 computing round = 1 OpenAI API call

**Example** for **GPT4** model performing action on **10 pages** of text:

```
Page estimate per round for GPT4 model:
* 1 page (12.5%) reserved for output by Assistant
* up to 7 pages ( the rest) for input
So the input text of 10 pages (over 7 pages) is big,
and is split into parts to shorten each part first.

Splitting is done by Turbo model:
* 0.0021 USD per 1 text page (both input and output)
Page estimate per round is total of 4 for Turbo model:
* 0.5 pages (12.5%) reserved for output by Assistant
* up to 3.5 pages (the rest) for input

Part 1 (max 3.5 pages for Turbo):
* shortened part size is a full output size: 0.5 pages
* part size before shortening, input size: 3.5 pages
* the part is shortened in 1 round
* shortening cost: total 4 pages = 0.0084 USD

Part 2 (max 3.5 pages for Turbo):
* shortened part size is a full output size: 0.5 pages
* part size before shortening, input size: 3.5 pages
* the part is shortened in 1 round
* shortening cost: total 4 pages = 0.0084 USD

Part 3 (remaining 3 pages):
* shortened part size is a full output size: 0.5 pages
* part size before shortening, input size: 3 pages
* the part is shortened in 1 round
* shortening cost: total 3.5 pages = 0.0074 USD

Joined shortened parts: 3 * 0.5 = 1.5 pages.
It is no more than 7 pages limit for GPT4, so not big,
and is ready to be processed by GPT4.
Costs for GPT4 model: 
* 0.031 USD per 1 input text page
* 0.062 USD per 1 output text page

Performing action by GPT4:
* final output size is a full output size: 1 page
* input size: 1.5 pages
* action is performed in 1 round
* action cost, input: 1.5 pages = 0.047 USD
* action cost, output: 1 page = 0.062 USD
* action cost, total: 0.047 + 0.062 = 0.11 USD

Total rounds: 4
Total cost: 0.0084 + 0.0084 + 0.0074 + 0.11 = 0.14 USD
```

Part overlap size and action/shortening command sizes are considered negligibly small relatively to
the part/text sizes in the estimates below.

| Input size \ Models    | Turbo                  | DaVinci                | GPT4                   |
|------------------------|------------------------|------------------------|------------------------|
| OpenAI model name      | `gpt-3.5-turbo`        | `text-davinci-003`     | `gpt-4`                |
| _Input / output pages_ | _up to 3.5 / 0.5_      | _up to 3.5 / 0.5_      | _up to 7 / 1_          |
| 1 page                 | 1 round, 0.0031 USD    | 1 round, 0.031 USD     | 1 round, 0.093 USD     |
| 2 pages                | 1 round, 0.0052 USD    | 1 round, 0.052 USD     | 1 round, 0.13 USD      |
| 5 pages                | 3 rounds, 0.015 USD    | 3 rounds, 0.044 USD    | 1 round, 0.22 USD      |
| 10 pages               | 4 rounds, 0.027 USD    | 4 rounds, 0.065 USD    | 4 rounds, 0.14 USD     |
| 20 pages               | 7 rounds, 0.051 USD    | 7 rounds, 0.12 USD     | 7 rounds, 0.20 USD     |
| 50 pages               | 21 rounds, 0.17 USD`*` | 21 rounds, 0.24 USD`*` | 16 rounds, 0.40 USD    |
| 100 pages              | 35 rounds, 0.29 USD`*` | 35 rounds, 0.36 USD`*` | 35 rounds, 0.56 USD`*` |

The number of rounds given in addition to cost may be useful for estimating total processing time.

>`*  After the first stage of shortening, the resulting text in these cases is still too long for the action performing model and has to be shortened again. Because of higher possible variation of the secord-stage shortened text size, it's estimated as maximum input size for the action performing model.`

_Note: The text splitting technique may be subject for replacement with a method with better
capability._

### Text dictation

When the microphone button is pressed, Assistant starts recording voice from the default microphone`*`. 
The microphone button is red when recording. Press it again to finish. 

Assistant gets the dictated text using OpenAI speech-to-text Whisper model, and adds the dictated text in the end of the text input form. 

#### Duration and cost estimates for text dictation:

| Costs \ Duration          | 1 minute       | 12.5 minutes (max) |
|---------------------------|----------------|--------------------|
| OpenAI Whisper model name | `large-v2`     | `large-v2`         |
| Cost estimate             | 0.006 USD`**`  | 0.075 USD          |
| Data usage (uploads)      | 2 MB           | 25 MB`**`          |

>`* If no microphone detected, the microphone button will be hidden. Reopen Assistant to detect again.`

>`** Primary source of data for recording per-minute cost and max size: OpenAI.`

### Capturing text from screen

Click the screenshot button or `Ctrl`+`PrintScreen` to view your screen capture.
Select a rectangular area with text of interest by clicking and dragging. 
Text from that area will be put in the end of the text input form.

Text capturing uses offline free software`*`. For convenience, Assistant will hide its window and surround the screenshot with a border. 

>`* See Optional section below. If the optional software is unavailable, the screenshot button will be hidden. Reopen Assistant window to recheck availability.`


## Running the app

### Desktop, Linux

Requirements:

* Java

Optional:

* `tesseract-ocr` package installed, for capturing text from screen. Assistant checks if `/usr/share/tesseract-ocr/5/tessdata` directory exists.

Initial config:

* Write an OpenAI API key to `~/.openai-credentials`

Run:

```
./gradlew run
```

## Development

UI: Jetpack Compose

## License

[MIT License](LICENSE)
