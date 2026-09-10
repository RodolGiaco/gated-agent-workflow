package io.github.rodolgiaco.gaw;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SlugTest {

  @Test
  void lowercasesTheTitle() {
    assertEquals("readme", Slug.from("README"));
    assertEquals("addslugutility", Slug.from("AddSlugUtility"));
  }

  @Test
  void collapsesEveryRunOfOtherCharactersIntoOneHyphen() {
    assertEquals("fix-the-ci-cd-pipeline", Slug.from("Fix   the __ CI/CD  pipeline"));
    assertEquals("release-v2-0-now", Slug.from("Release v2.0 now"));
  }

  @Test
  void leavesNoHyphenAtEitherEnd() {
    assertEquals("add-the-runner", Slug.from("  -- Add the runner --  "));
    assertEquals("java-21", Slug.from("...Java 21!!!"));
  }

  @Test
  void cutsToFortyCharactersWithoutLeavingATrailingHyphen() {
    // Cut inside a word: the result is exactly MAX_LENGTH characters. This is
    // the title of the issue that asked for this class, so the expected value
    // is the slug in the branch name the runner derived for it.
    String insideAWord =
        Slug.from("Add a Slug utility that turns an issue title into a branch slug");
    assertEquals("add-a-slug-utility-that-turns-an-issue-t", insideAWord);
    assertEquals(Slug.MAX_LENGTH, insideAWord.length());

    // Cut right after a word: character 40 is the hyphen that followed it, so
    // it goes and the result is one character shorter than the limit.
    String afterAWord = Slug.from("Derive a branch slug from the issue and the title");
    assertEquals("derive-a-branch-slug-from-the-issue-and", afterAWord);
    assertEquals(Slug.MAX_LENGTH - 1, afterAWord.length());
    assertTrue(afterAWord.length() <= Slug.MAX_LENGTH, "the slug outgrew the limit");
  }

  @Test
  void rejectsANullTitle() {
    assertThrows(IllegalArgumentException.class, () -> Slug.from(null));
  }

  @Test
  void rejectsATitleThatProducesAnEmptyResult() {
    assertThrows(IllegalArgumentException.class, () -> Slug.from(""));
    assertThrows(IllegalArgumentException.class, () -> Slug.from("   "));
    assertThrows(IllegalArgumentException.class, () -> Slug.from("!!! ???"));
  }
}
