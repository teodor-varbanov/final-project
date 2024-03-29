package lila.relay

import play.api.data.*
import play.api.data.Forms.*

import lila.common.Form.{ cleanText, formatter, into }
import lila.security.Granter
import lila.user.Me
import lila.i18n.LangForm

final class RelayTourForm:

  import RelayTourForm.*

  val spotlightMapping =
    mapping("enabled" -> boolean, "lang" -> LangForm.popularLanguages.mapping, "title" -> optional(text))(
      RelayTour.Spotlight.apply
    )(unapply)

  val form = Form(
    mapping(
      "name"            -> cleanText(minLength = 3, maxLength = 80),
      "description"     -> cleanText(minLength = 3, maxLength = 400),
      "markdown"        -> optional(cleanText(maxLength = 20_000).into[Markdown]),
      "tier"            -> optional(number(min = RelayTour.Tier.NORMAL, max = RelayTour.Tier.BEST)),
      "autoLeaderboard" -> boolean,
      "players"         -> optional(of(formatter.stringFormatter[RelayPlayers](_.text, RelayPlayers.apply))),
      "spotlight"       -> optional(spotlightMapping)
    )(Data.apply)(unapply)
  )

  def create = form

  def edit(t: RelayTour) = form fill Data.make(t)

object RelayTourForm:

  case class Data(
      name: String,
      description: String,
      markup: Option[Markdown],
      tier: Option[RelayTour.Tier],
      autoLeaderboard: Boolean,
      players: Option[RelayPlayers],
      spotlight: Option[RelayTour.Spotlight]
  ):

    def update(tour: RelayTour)(using Me) =
      tour
        .copy(
          name = name,
          description = description,
          markup = markup,
          tier = tier ifTrue Granter(_.Relay),
          autoLeaderboard = autoLeaderboard,
          players = players,
          spotlight = spotlight.filterNot(_.isEmpty)
        )
        .reAssignIfOfficial

    def make(using me: Me) =
      RelayTour(
        _id = RelayTour.makeId,
        name = name,
        description = description,
        markup = markup,
        ownerId = me,
        tier = tier ifTrue Granter(_.Relay),
        active = false,
        createdAt = nowInstant,
        syncedAt = none,
        autoLeaderboard = autoLeaderboard,
        players = players,
        spotlight = spotlight.filterNot(_.isEmpty)
      ).reAssignIfOfficial

  object Data:

    def make(tour: RelayTour) =
      Data(
        name = tour.name,
        description = tour.description,
        markup = tour.markup,
        tier = tour.tier,
        autoLeaderboard = tour.autoLeaderboard,
        players = tour.players,
        spotlight = tour.spotlight
      )
