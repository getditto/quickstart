use crossterm::event::Event;
use ratatui::{
    buffer::Buffer,
    layout::{Constraint, Rect},
    style::{Style, Stylize as _},
    text::{Line, Span, Text},
    widgets::{Cell, ListItem, Row, StatefulWidget, Table, TableState},
};

use crate::{config::Profile, key};

use super::TuiActions;

pub struct ProfilesContainer {}

pub struct ProfilesState {
    pub profiles_list_state: ProfilesListState,
}

impl ProfilesContainer {
    pub fn new() -> Self {
        Self {}
    }
}

impl StatefulWidget for ProfilesContainer {
    type State = ProfilesState;

    fn render(self, area: Rect, buf: &mut Buffer, state: &mut Self::State) {
        ProfilesList::new().render(area, buf, &mut state.profiles_list_state);
    }
}

impl ProfilesState {
    pub fn new() -> Self {
        Self {
            profiles_list_state: ProfilesListState::new(),
        }
    }

    pub fn handle_event(&mut self, actions: TuiActions<'_>, event: &Event) {
        match event {
            key!(Up) | key!(Char('k')) => {
                self.profiles_list_state.table_state.select_previous();
            }
            key!(Down) | key!(Char('j')) => {
                self.profiles_list_state.table_state.select_next();
            }
            key!(Enter) => {
                let maybe_profile = self.profiles_list_state.selected_profile();
                if let Some(profile) = maybe_profile {
                    actions.enter_profile(profile);
                }
            }
            _ => {}
        };
    }

    pub fn breadcrumbs(&self) -> Vec<Span<'_>> {
        vec!["Profiles ".bold()]
    }
}

pub struct ProfilesList {}

pub struct ProfilesListState {
    pub items: Vec<ProfileListItem>,
    pub table_state: TableState,
}

impl ProfilesListState {
    pub fn selected_profile(&self) -> Option<Profile> {
        let selected = self.table_state.selected()?;
        let item = self.items.get(selected)?;
        Some(item.profile.clone())
    }
}

pub struct ProfileListItem {
    profile: Profile,
}

impl ProfilesList {
    pub fn new() -> Self {
        Self {}
    }
}

impl StatefulWidget for ProfilesList {
    type State = ProfilesListState;

    fn render(self, area: Rect, buf: &mut Buffer, state: &mut Self::State) {
        let header = ["Name", "AppID"]
            .into_iter()
            .map(Cell::from)
            .collect::<Row>()
            .style(Style::new().bold())
            .height(1);
        let rows = state.items.iter().map(|it| {
            [
                it.profile.name.as_deref().unwrap_or("<no nickname>"),
                &*it.profile.app_id,
            ]
            .into_iter()
            .map(|col| Cell::from(Text::from(col)))
            .collect::<Row>()
        });

        let table = Table::new(rows, Constraint::from_percentages([40, 60]))
            .header(header)
            .highlight_symbol("❯❯ ")
            .highlight_style(Style::new().bold().blue());
        StatefulWidget::render(table, area, buf, &mut state.table_state);
    }
}

impl ProfilesListState {
    pub fn new() -> Self {
        Self {
            items: Vec::new(),
            table_state: TableState::new().with_selected(Some(0)),
        }
    }
}

impl ProfileListItem {
    pub fn from_profile(profile: &Profile) -> Self {
        Self {
            profile: profile.clone(),
        }
    }
}

impl From<&ProfileListItem> for ListItem<'_> {
    fn from(value: &ProfileListItem) -> Self {
        let name = value.profile.name.as_deref().unwrap_or("");
        let line = format!("{name:>16}: {}", value.profile.app_id);
        ListItem::new(Line::raw(line))
    }
}
