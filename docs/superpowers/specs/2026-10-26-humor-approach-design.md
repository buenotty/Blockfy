# Humor-first interruptions (design)

Date: 2026-10-26. Status: implemented in 1.10.0.

## Intent
Blockfy used a tone of guilt and loss ("you will lose N days", "stronger than a cheap dopamine loop").
For people who use short video or pornography to numb a feeling, that tone tends to cause rebellion
or a "might as well" relapse. The goal now: break the loop with a short, light interruption that
brings attention back, then points to something useful. The user always stays free to leave.

## What the research says (and does not say)
- Shame, more than guilt, predicted relapse in newly sober drinkers (Tracy & Randles, 2013; small sample).
- Threatening health messages often trigger reactance; one experiment found humor reduced it more than
  fear, but the evidence for humor is narrow, and humor may lower perceived risk.
- The strongest result for blockers (one sec, PNAS) is the pause plus an easy way out. A separate
  experiment found the reflection message itself did not help. So humor is the carrier of the pause,
  not the cure.
- Problematic pornography use is linked to emotion regulation: the use often numbs a feeling. In small
  studies people could down-regulate negative emotion by reappraisal when instructed.
- Urge surfing showed a small effect on behavior. The "urge lasts 20-30 minutes" rule has no study
  behind it, so the app never promises a duration.
- Humor styles: affiliative and self-enhancing humor go with well-being; aggressive and self-defeating
  humor do not. Rule: laugh with the user, never at them.

## Decisions
1. Interruption is a window owned by the Accessibility service (TYPE_ACCESSIBILITY_OVERLAY). Verified on
   an Android 17 emulator over the real YouTube app. No "draw over other apps" permission needed.
   Android's docs list SYSTEM_ALERT_WINDOW, not Accessibility, as a background-start exemption, so the
   window is added directly instead of starting an Activity.
2. Short-video blocks show a trick question where both answers are wrong, a light result, and one small
   useful suggestion. Six challenges ship (one illustrated: the mechanical bull). The bank is data.
3. The adult shield uses a calmer "quick pause" with no quiz, a suggestion, and the CVV line (188).
4. The app leaves the blocked screen first, then shows the window. The user is never trapped: one tap
   closes it, and it closes itself after 30 seconds. A 15 second cooldown avoids stacking.
5. No bypass button: the block is the user's own rule. Loosening a rule keeps its existing friction.
6. Illustrations are original vector art (no third-party memes), to avoid copyright and image-rights risk.
7. All guilt framing was rewritten: "clean days" became "steady days", the stake dialog no longer says
   "you will lose", the pornography concept is about the job the habit does and how to swap it.
8. A switch in Settings ("Funny challenges") returns to the plain alert.

## Non-goals
- No claim that humor treats addiction. No time promises. No data leaves the device.

## Risks and mitigations
- The challenge could become another game: short, one per episode, no rewards.
- Jokes wear out: bank is data, more can be added without code.
- Humor can sound like mockery to someone in pain: calmer tone for the adult shield, help line shown.

## Left for later
- A "my alternatives" list so the suggestion is personal.
- More illustrated challenges, and testing which ones the beta testers like.
