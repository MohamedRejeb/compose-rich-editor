package com.mohamedrejeb.richeditor.sample.common.richeditor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.sample.common.components.RichTextStyleRow
import com.mohamedrejeb.richeditor.sample.common.ui.theme.ComposeRichEditorTheme
import com.mohamedrejeb.richeditor.ui.BasicRichTextEditor
import com.mohamedrejeb.richeditor.ui.material3.OutlinedRichTextEditor
import com.mohamedrejeb.richeditor.ui.material3.RichText
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RichEditorContent() {
    val navigator = LocalNavigator.currentOrThrow

    val basicRichTextState = rememberRichTextState()
    val richTextState = rememberRichTextState()
    val outlinedRichTextState = rememberRichTextState()

    LaunchedEffect(Unit) {
        richTextState.setHtml(
            """
            <p><b>RichTextEditor</b> is a <i>composable</i> that allows you to edit <u>rich text</u> content.</p>
            """.trimIndent()
        )

        outlinedRichTextState.setMarkdown("""**Down the Rabbit-Hole**

  Alice was beginning to get very tired of sitting by her sister
on the bank, and of having nothing to do:  once or twice she had
peeped into the book her sister was reading, but it had no
pictures or conversations in it, `and what is the use of a book,'
thought Alice `without pictures or conversation?'
xxxxxxx
  So she was considering in her own mind (as well as she could,
for the hot day made her feel very sleepy and stupid), whether
the pleasure of making a daisy-chain would be worth the trouble
of getting up and picking the daisies, when suddenly a White
Rabbit with pink eyes ran close by her.
asd
  There was nothing so VERY remarkable in that; nor did Alice
think it so VERY much out of the way to hear the Rabbit say to
itself, `Oh dear!  Oh dear!  I shall be late!'  (when she thought
it over afterwards, it occurred to her that she ought to have
wondered at this, but at the time it all seemed quite natural);
but when the Rabbit actually TOOK A WATCH OUT OF ITS WAISTCOAT-
POCKET, and looked at it, and then hurried on, Alice started to
her feet, for it flashed across her mind that she had never
before seen a rabbit with either a waistcoat-pocket, or a watch to
take out of it, and burning with curiosity, she ran across the
field after it, and fortunately was just in time to see it pop
down a large rabbit-hole under the hedge.

  In another moment down went Alice after it, never once
considering how in the world she was to get out again.

  The rabbit-hole went straight on like a tunnel for some way,
and then dipped suddenly down, so suddenly that Alice had not a
moment to think about stopping herself before she found herself
falling down a very deep well.

  Either the well was very deep, or she fell very slowly, for she
had plenty of time as she went down to look about her and to
wonder what was going to happen next.  First, she tried to look
down and make out what she was coming to, but it was too dark to
see anything; then she looked at the sides of the well, and
noticed that they were filled with cupboards and book-shelves;
here and there she saw maps and pictures hung upon pegs.  She
took down a jar from one of the shelves as she passed; it was
labelled `ORANGE MARMALADE', but to her great disappointment it
was empty:  she did not like to drop the jar for fear of killing
somebody, so managed to put it into one of the cupboards as she
fell past it.

  `Well!' thought Alice to herself, `after such a fall as this, I
shall think nothing of tumbling down stairs!  How brave they'll
all think me at home!  Why, I wouldn't say anything about it,
even if I fell off the top of the house!' (Which was very likely
true.)

  Down, down, down.  Would the fall NEVER come to an end!  `I
wonder how many miles I've fallen by this time?' she said aloud.
`I must be getting somewhere near the centre of the earth.  Let
me see:  that would be four thousand miles down, I think--' (for,
you see, Alice had learnt several things of this sort in her
lessons in the schoolroom, and though this was not a VERY good
opportunity for showing off her knowledge, as there was no one to
listen to her, still it was good practice to say it over) `--yes,
that's about the right distance--but then I wonder what Latitude
or Longitude I've got to?'  (Alice had no idea what Latitude was,
or Longitude either, but thought they were nice grand words to
say.)

  Presently she began again.  `I wonder if I shall fall right
THROUGH the earth!  How funny it'll seem to come out among the
people that walk with their heads downward!  The Antipathies, I
think--' (she was rather glad there WAS no one listening, this
time, as it didn't sound at all the right word) `--but I shall
have to ask them what the name of the country is, you know.
Please, Ma'am, is this New Zealand or Australia?' (and she tried
to curtsey as she spoke--fancy CURTSEYING as you're falling
through the air!  Do you think you could manage it?)  `And what
an ignorant little girl she'll think me for asking!  No, it'll
never do to ask:  perhaps I shall see it written up somewhere.'

  Down, down, down.  There was nothing else to do, so Alice soon
began talking again.  `Dinah'll miss me very much to-night, I
should think!'  (Dinah was the cat.)  `I hope they'll remember
her saucer of milk at tea-time.  Dinah my dear!  I wish you were
down here with me!  There are no mice in the air, I'm afraid, but
you might catch a bat, and that's very like a mouse, you know.
But do cats eat bats, I wonder?'  And here Alice began to get
rather sleepy, and went on saying to herself, in a dreamy sort of
way, `Do cats eat bats?  Do cats eat bats?' and sometimes, `Do
bats eat cats?' for, you see, as she couldn't answer either
question, it didn't much matter which way she put it.  She felt
that she was dozing off, and had just begun to dream that she
was walking hand in hand with Dinah, and saying to her very
earnestly, `Now, Dinah, tell me the truth:  did you ever eat a
bat?' when suddenly, thump! thump! down she came upon a heap of
sticks and dry leaves, and the fall was over.

  Alice was not a bit hurt, and she jumped up on to her feet in a
moment:  she looked up, but it was all dark overhead; before her
was another long passage, and the White Rabbit was still in
sight, hurrying down it.  There was not a moment to be lost:
away went Alice like the wind, and was just in time to hear it
say, as it turned a corner, `Oh my ears and whiskers, how late
it's getting!'  She was close behind it when she turned the
corner, but the Rabbit was no longer to be seen:  she found
herself in a long, low hall, which was lit up by a row of lamps
hanging from the roof.

  There were doors all round the hall, but they were all locked;
and when Alice had been all the way down one side and up the
other, trying every door, she walked sadly down the middle,
wondering how she was ever to get out again.

  Suddenly she came upon a little three-legged table, all made of
solid glass; there was nothing on it except a tiny golden key,
and Alice's first thought was that it might belong to one of the
doors of the hall; but, alas! either the locks were too large, or
the key was too small, but at any rate it would not open any of
them.  However, on the second time round, she came upon a low
curtain she had not noticed before, and behind it was a little
door about fifteen inches high:  she tried the little golden key
in the lock, and to her great delight it fitted!

  Alice opened the door and found that it led into a small
passage, not much larger than a rat-hole:  she knelt down and
looked along the passage into the loveliest garden you ever saw.
How she longed to get out of that dark hall, and wander about
among those beds of bright flowers and those cool fountains, but
she could not even get her head though the doorway; `and even if
my head would go through,' thought poor Alice, `it would be of
very little use without my shoulders.  Oh, how I wish
I could shut up like a telescope!  I think I could, if I only
know how to begin.'  For, you see, so many out-of-the-way things
had happened lately, that Alice had begun to think that very few
things indeed were really impossible.""".trimIndent())
    }

    ComposeRichEditorTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Compose Rich Editor") },
                    navigationIcon = {
                        IconButton(
                            onClick = { navigator.pop() }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            },
            modifier = Modifier
                .fillMaxSize()
        ) { paddingValue ->
            LazyColumn(
                contentPadding = paddingValue,
                modifier = Modifier
                    .padding(paddingValue)
                    .windowInsetsPadding(WindowInsets.ime)
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // BasicRichTextEditor
                item {
                    Text(
                        text = "BasicRichTextEditor:",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                item {
                    Spacer(Modifier.height(8.dp))
                }

                item {
                    RichTextStyleRow(
                        modifier = Modifier.fillMaxWidth(),
                        state = basicRichTextState,
                    )
                }

                item {
                    BasicRichTextEditor(
                        modifier = Modifier.fillMaxWidth(),
                        state = basicRichTextState,
                    )
                }

                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))
                }

                // RichTextEditor
                item {
                    Text(
                        text = "RichTextEditor:",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                item {
                    Spacer(Modifier.height(8.dp))
                }

                item {
                    RichTextStyleRow(
                        modifier = Modifier.fillMaxWidth(),
                        state = richTextState,
                    )
                }

                item {
                    RichTextEditor(
                        modifier = Modifier.fillMaxWidth(),
                        state = richTextState,
                        readOnly = true,
                    )
                }

                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))
                }

                // OutlinedRichTextEditor
                item {
                    Text(
                        text = "OutlinedRichTextEditor:",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                item {
                    Spacer(Modifier.height(8.dp))
                }

                item {
                    RichTextStyleRow(
                        modifier = Modifier.fillMaxWidth(),
                        state = outlinedRichTextState,
                    )
                }

                item {
                    RichTextEditor(
                        modifier = Modifier.fillMaxWidth(),
                        state = outlinedRichTextState,
                    )
                }

                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))
                }

                // RichText
                item {
                    Text(
                        text = "RichText:",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                item {
                    Spacer(Modifier.height(8.dp))
                }

                item {
                    RichText(
                        modifier = Modifier.fillMaxWidth(),
                        state = richTextState,
                    )
                }
            }
        }
    }
}