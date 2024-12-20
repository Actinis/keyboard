package text

import io.actinis.remote.keyboard.data.state.model.InputState
import io.actinis.remote.keyboard.data.text.model.LanguageRules
import io.actinis.remote.keyboard.data.text.repository.LanguagesRepository
import io.actinis.remote.keyboard.domain.text.TextAnalyzerImpl
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TextAnalyzerTest {
    private val languagesRepository = mockk<LanguagesRepository>()
    private val textAnalyzer = TextAnalyzerImpl(languagesRepository)

    private val englishRules = LanguageRules(
        wordSeparators = setOf(' ', '\t', '\n', ',', ';', ':', '!', '?', '.'),
        sentenceEndings = setOf('.', '!', '?'),
        abbreviations = setOf("Mr.", "Mrs.", "Dr.", "etc.", "i.e.", "e.g.")
    )

    @Before
    fun setup() {
        coEvery {
            languagesRepository.getLanguageRules("en_US")
        } returns englishRules
    }

    @Test
    fun `isNewWordPosition - empty text`() {
        val inputState = createInputState(
            text = "",
            selectionStart = 0,
            selectionEnd = 0
        )

        val result = runBlocking { textAnalyzer.isNewWordPosition(inputState, "en_US") }
        assertTrue(result)
    }

    @Test
    fun `isNewWordPosition - cursor at start`() {
        val inputState = createInputState(
            text = "sample text",
            selectionStart = 0,
            selectionEnd = 0
        )

        val result = runBlocking { textAnalyzer.isNewWordPosition(inputState, "en_US") }
        assertTrue(result)
    }

    @Test
    fun `isNewWordPosition - cursor at end`() {
        val inputState = createInputState(
            text = "sample text",
            selectionStart = 11,
            selectionEnd = 11
        )

        val result = runBlocking { textAnalyzer.isNewWordPosition(inputState, "en_US") }
        assertFalse(result)
    }

    @Test
    fun `isNewWordPosition - cursor at end with space`() {
        val inputState = createInputState(
            text = "sample text ",
            selectionStart = 12,
            selectionEnd = 12
        )

        val result = runBlocking { textAnalyzer.isNewWordPosition(inputState, "en_US") }
        assertTrue(result)
    }

    @Test
    fun `isNewWordPosition - cursor in middle of word`() {
        val inputState = createInputState(
            text = "sample text",
            selectionStart = 3,
            selectionEnd = 3
        )

        val result = runBlocking { textAnalyzer.isNewWordPosition(inputState, "en_US") }
        assertFalse(result)
    }

    @Test
    fun `isNewWordPosition - cursor after word before space`() {
        val inputState = createInputState(
            text = "sample text",
            selectionStart = 6,
            selectionEnd = 6
        )

        val result = runBlocking { textAnalyzer.isNewWordPosition(inputState, "en_US") }
        assertFalse(result)
    }

    @Test
    fun `isNewWordPosition - cursor after space before word`() {
        val inputState = createInputState(
            text = "sample text",
            selectionStart = 7,
            selectionEnd = 7
        )

        val result = runBlocking { textAnalyzer.isNewWordPosition(inputState, "en_US") }
        assertTrue(result)
    }

    @Test
    fun `isNewWordPosition - multiple spaces`() {
        val inputState = createInputState(
            text = "sample   text",
            selectionStart = 8,
            selectionEnd = 8
        )

        val result = runBlocking { textAnalyzer.isNewWordPosition(inputState, "en_US") }
        assertTrue(result)
    }

    @Test
    fun `isNewWordPosition - after punctuation`() {
        val inputState = createInputState(
            text = "sample, text",
            selectionStart = 7,
            selectionEnd = 7
        )

        val result = runBlocking { textAnalyzer.isNewWordPosition(inputState, "en_US") }
        assertTrue(result)
    }

    @Test
    fun `isNewWordPosition - before punctuation`() {
        val inputState = createInputState(
            text = "sample, text",
            selectionStart = 6,
            selectionEnd = 6
        )

        val result = runBlocking { textAnalyzer.isNewWordPosition(inputState, "en_US") }
        assertFalse(result)
    }

    // Sentence boundary tests
    @Test
    fun `isNewSentencePosition - empty text`() {
        val inputState = createInputState(
            text = "",
            selectionStart = 0,
            selectionEnd = 0
        )

        val result = runBlocking { textAnalyzer.isNewSentencePosition(inputState, "en_US") }
        assertTrue(result)
    }

    @Test
    fun `isNewSentencePosition - after period`() {
        val inputState = createInputState(
            text = "First sentence. Second sentence",
            selectionStart = 15,
            selectionEnd = 15
        )

        val result = runBlocking { textAnalyzer.isNewSentencePosition(inputState, "en_US") }
        assertTrue(result)
    }

    @Test
    fun `isNewSentencePosition - after exclamation mark`() {
        val inputState = createInputState(
            text = "Hello! World",
            selectionStart = 6,
            selectionEnd = 6
        )

        val result = runBlocking { textAnalyzer.isNewSentencePosition(inputState, "en_US") }
        assertTrue(result)
    }

    @Test
    fun `isNewSentencePosition - after question mark`() {
        val inputState = createInputState(
            text = "How are you? I am fine",
            selectionStart = 12,
            selectionEnd = 12
        )

        val result = runBlocking { textAnalyzer.isNewSentencePosition(inputState, "en_US") }
        assertTrue(result)
    }

    @Test
    fun `isNewSentencePosition - after abbreviation`() {
        val inputState = createInputState(
            text = "Mr. Smith",
            selectionStart = 3,
            selectionEnd = 3
        )

        val result = runBlocking { textAnalyzer.isNewSentencePosition(inputState, "en_US") }
        assertFalse(result)
    }

    @Test
    fun `isNewSentencePosition - multiple punctuation marks`() {
        val inputState = createInputState(
            text = "Really?! Yes",
            selectionStart = 8,
            selectionEnd = 8
        )

        val result = runBlocking { textAnalyzer.isNewSentencePosition(inputState, "en_US") }
        assertTrue(result)
    }

    @Test
    fun `isNewSentencePosition - multiple spaces after sentence`() {
        val inputState = createInputState(
            text = "First sentence.   Second sentence",
            selectionStart = 17,
            selectionEnd = 17
        )

        val result = runBlocking { textAnalyzer.isNewSentencePosition(inputState, "en_US") }
        assertTrue(result)
    }

    @Test
    fun `isNewSentencePosition - middle of sentence`() {
        val inputState = createInputState(
            text = "This is a sentence",
            selectionStart = 8,
            selectionEnd = 8
        )

        val result = runBlocking { textAnalyzer.isNewSentencePosition(inputState, "en_US") }
        assertFalse(result)
    }

    @Test
    fun `isNewSentencePosition - after comma`() {
        val inputState = createInputState(
            text = "Hello, world",
            selectionStart = 6,
            selectionEnd = 6
        )

        val result = runBlocking { textAnalyzer.isNewSentencePosition(inputState, "en_US") }
        assertFalse(result)
    }

    @Test
    fun `isNewSentencePosition - end of text`() {
        val inputState = createInputState(
            text = "This is the end.",
            selectionStart = 16,
            selectionEnd = 16
        )

        val result = runBlocking { textAnalyzer.isNewSentencePosition(inputState, "en_US") }
        assertTrue(result)
    }

    @Test
    fun `isNewSentencePosition - abbreviation`() {
        val inputState = createInputState(
            text = "This is the end, mr.",
            selectionStart = 20,
            selectionEnd = 20
        )

        val result = runBlocking { textAnalyzer.isNewSentencePosition(inputState, "en_US") }
        assertFalse(result)
    }

    private fun createInputState(
        text: String,
        selectionStart: Int,
        selectionEnd: Int,
        isMultiline: Boolean = false,
        actionType: InputState.ActionType = InputState.ActionType.NONE,
        isPersonalizedLearningEnabled: Boolean = true,
    ): InputState = InputState.Text(
        text = text,
        selectionStart = selectionStart,
        selectionEnd = selectionEnd,
        isMultiline = isMultiline,
        actionType = actionType,
        isPersonalizedLearningEnabled = isPersonalizedLearningEnabled
    )
}
