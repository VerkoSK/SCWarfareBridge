package dev.scwarfarebridge.client;

import dev.scwarfarebridge.nation.DiplomacyState;
import dev.scwarfarebridge.nation.NationRank;
import dev.scwarfarebridge.network.C2SCreateNationPacket;
import dev.scwarfarebridge.network.C2SDiplomacyPacket;
import dev.scwarfarebridge.network.C2SJoinNationPacket;
import dev.scwarfarebridge.network.C2SLeaveNationPacket;
import dev.scwarfarebridge.network.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class NationScreen extends Screen {

    private static final int BG_WIDTH = 300;
    private static final int BG_HEIGHT = 210;
    private static final int HEADER_H = 24;
    private static final int TAB_H = 20;
    private static final int CONTENT_Y_START = HEADER_H + TAB_H + 4;

    // Colors
    private static final int COLOR_BG       = 0xCC101820;
    private static final int COLOR_HEADER   = 0xCC0D1520;
    private static final int COLOR_BORDER   = 0xFF2A4060;
    private static final int COLOR_TAB_ACTIVE   = 0xCC2A4A6A;
    private static final int COLOR_TAB_HOVER    = 0xCC1E3550;
    private static final int COLOR_TAB_INACTIVE = 0xCC141E2A;
    private static final int COLOR_ROW_EVEN = 0x22FFFFFF;
    private static final int COLOR_ROW_ODD  = 0x11FFFFFF;
    private static final int COLOR_TITLE    = 0xFFE8D080;
    private static final int COLOR_TEXT     = 0xFFCCDDEE;
    private static final int COLOR_SUBTEXT  = 0xFF8899AA;
    private static final int COLOR_ALLIED   = 0xFF44CC44;
    private static final int COLOR_NEUTRAL  = 0xFF888888;
    private static final int COLOR_WAR      = 0xFFCC4444;

    private enum Tab { NATIONS, MY_NATION, CREATE }

    private Tab currentTab = Tab.NATIONS;
    private int bgX, bgY;

    // Tab 1: Nations
    private int nationsScroll = 0;

    // Tab 3: Create
    private EditBox nameField;
    private DyeColor selectedColor = DyeColor.BLUE;
    private Button createButton;

    // Tab 2: My Nation
    private int membersScroll = 0;
    private int diplomacyScroll = 0;

    public NationScreen() {
        super(Component.translatable("screen.scwarfarebridge.nations"));
    }

    @Override
    protected void init() {
        bgX = (width - BG_WIDTH) / 2;
        bgY = (height - BG_HEIGHT) / 2;

        nameField = new EditBox(font, bgX + 10, bgY + CONTENT_Y_START + 20, 200, 18,
                Component.translatable("screen.scwarfarebridge.nation_name"));
        nameField.setMaxLength(24);
        nameField.setHint(Component.literal("e.g. Slovakia"));
        addWidget(nameField);

        createButton = Button.builder(Component.translatable("screen.scwarfarebridge.create_nation"),
                        btn -> doCreateNation())
                .bounds(bgX + BG_WIDTH / 2 - 60, bgY + BG_HEIGHT - 30, 120, 18)
                .build();
        addWidget(createButton);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);

        // Background panel
        g.fill(bgX, bgY, bgX + BG_WIDTH, bgY + BG_HEIGHT, COLOR_BG);
        drawBorder(g, bgX, bgY, BG_WIDTH, BG_HEIGHT);

        // Header bar
        g.fill(bgX, bgY, bgX + BG_WIDTH, bgY + HEADER_H, COLOR_HEADER);
        g.drawCenteredString(font, title, bgX + BG_WIDTH / 2, bgY + 7, COLOR_TITLE);

        drawTabs(g, mx, my);

        int cx = bgX + 6;
        int cy = bgY + CONTENT_Y_START;
        int cw = BG_WIDTH - 12;
        int ch = BG_HEIGHT - CONTENT_Y_START - 6;

        switch (currentTab) {
            case NATIONS   -> renderNationsTab(g, cx, cy, cw, ch, mx, my);
            case MY_NATION -> renderMyNationTab(g, cx, cy, cw, ch, mx, my);
            case CREATE    -> renderCreateTab(g, cx, cy, cw, ch, mx, my);
        }

        super.render(g, mx, my, pt);
    }

    // -----------------------------------------------------------------------
    // Tabs
    // -----------------------------------------------------------------------

    private void drawTabs(GuiGraphics g, int mx, int my) {
        Tab[] tabs = Tab.values();
        int tabW = BG_WIDTH / tabs.length;
        int ty = bgY + HEADER_H;

        for (int i = 0; i < tabs.length; i++) {
            int tx = bgX + i * tabW;
            boolean active = currentTab == tabs[i];
            boolean hovered = mx >= tx && mx < tx + tabW && my >= ty && my < ty + TAB_H;

            int bg = active ? COLOR_TAB_ACTIVE : (hovered ? COLOR_TAB_HOVER : COLOR_TAB_INACTIVE);
            g.fill(tx, ty, tx + tabW, ty + TAB_H, bg);
            if (active) {
                g.fill(tx, ty + TAB_H - 2, tx + tabW, ty + TAB_H, COLOR_BORDER);
            }
            drawBorder(g, tx, ty, tabW, TAB_H);

            String label = switch (tabs[i]) {
                case NATIONS   -> "§b" + translate("screen.scwarfarebridge.tab.nations");
                case MY_NATION -> "§e" + translate("screen.scwarfarebridge.tab.my_nation");
                case CREATE    -> "§a" + translate("screen.scwarfarebridge.tab.create");
            };
            g.drawCenteredString(font, label, tx + tabW / 2, ty + 6, active ? 0xFFFFFFFF : COLOR_SUBTEXT);
        }
    }

    // -----------------------------------------------------------------------
    // Tab 1: Nations list
    // -----------------------------------------------------------------------

    private void renderNationsTab(GuiGraphics g, int cx, int cy, int cw, int ch, int mx, int my) {
        List<ClientNationData.NationInfo> nations = ClientNationData.getNations();
        UUID localPlayer = Minecraft.getInstance().player != null
                ? Minecraft.getInstance().player.getUUID() : null;
        Optional<ClientNationData.NationInfo> myNation = localPlayer != null
                ? ClientNationData.getPlayerNation(localPlayer) : Optional.empty();

        if (nations.isEmpty()) {
            g.drawCenteredString(font, "§7No nations have been created yet.", cx + cw / 2, cy + ch / 2 - 5, COLOR_SUBTEXT);
            g.drawCenteredString(font, "§7Use the 'Create Nation' tab to found one!", cx + cw / 2, cy + ch / 2 + 8, COLOR_SUBTEXT);
            return;
        }

        int rowH = 26;
        int visibleRows = ch / rowH;
        int maxScroll = Math.max(0, nations.size() - visibleRows);
        nationsScroll = Math.min(nationsScroll, maxScroll);

        for (int i = 0; i < visibleRows && (i + nationsScroll) < nations.size(); i++) {
            ClientNationData.NationInfo nation = nations.get(i + nationsScroll);
            int ry = cy + i * rowH;

            g.fill(cx, ry, cx + cw, ry + rowH - 1, (i % 2 == 0) ? COLOR_ROW_EVEN : COLOR_ROW_ODD);

            int swatchColor = 0xFF000000 | nation.color().getFireworkColor();
            g.fill(cx + 2, ry + 5, cx + 16, ry + 19, swatchColor);
            drawBorder(g, cx + 2, ry + 5, 14, 14);

            g.drawString(font, "§f" + nation.name(), cx + 20, ry + 5, COLOR_TEXT);
            g.drawString(font, "§7" + nation.memberCount() + " members", cx + 20, ry + 14, COLOR_SUBTEXT);

            if (myNation.isPresent() && !myNation.get().id().equals(nation.id())) {
                DiplomacyState diplo = myNation.get().getDiplomacyWith(nation.id());
                int diploColor = switch (diplo) {
                    case ALLIED  -> COLOR_ALLIED;
                    case AT_WAR  -> COLOR_WAR;
                    case NEUTRAL -> COLOR_NEUTRAL;
                };
                String diploStr = switch (diplo) {
                    case ALLIED  -> "§a[Allied]";
                    case AT_WAR  -> "§c[War]";
                    case NEUTRAL -> "§7[Neutral]";
                };
                g.drawString(font, diploStr, cx + cw - 80, ry + 9, diploColor);
            } else if (myNation.isPresent() && myNation.get().id().equals(nation.id())) {
                g.drawString(font, "§6[Your Nation]", cx + cw - 90, ry + 9, 0xFFFFAA00);
            }

            // Join button — only if not in a nation and has an invite
            if (myNation.isEmpty() && localPlayer != null && nation.pendingInvites().contains(localPlayer)) {
                boolean btnHover = mx >= cx + cw - 42 && mx < cx + cw && my >= ry + 4 && my < ry + rowH - 4;
                g.fill(cx + cw - 42, ry + 4, cx + cw, ry + rowH - 4, btnHover ? 0xFF226622 : 0xFF1A4A1A);
                drawBorder(g, cx + cw - 42, ry + 4, 42, rowH - 8);
                g.drawCenteredString(font, "§aJoin", cx + cw - 21, ry + 9, 0xFFAAFFAA);
            }
        }

        if (nations.size() > visibleRows) {
            g.drawString(font, "§7" + (nationsScroll + 1) + "/" + nations.size(), cx + cw - 30, cy + ch - 10, COLOR_SUBTEXT);
        }
    }

    // -----------------------------------------------------------------------
    // Tab 2: My Nation
    // -----------------------------------------------------------------------

    private void renderMyNationTab(GuiGraphics g, int cx, int cy, int cw, int ch, int mx, int my) {
        UUID localPlayer = Minecraft.getInstance().player != null
                ? Minecraft.getInstance().player.getUUID() : null;
        if (localPlayer == null) return;

        Optional<ClientNationData.NationInfo> optNation = ClientNationData.getPlayerNation(localPlayer);

        if (optNation.isEmpty()) {
            g.drawCenteredString(font, "§7You are not in a nation.", cx + cw / 2, cy + ch / 2 - 6, COLOR_SUBTEXT);
            g.drawCenteredString(font, "§7Join one via the Nations tab or create your own!", cx + cw / 2, cy + ch / 2 + 6, COLOR_SUBTEXT);
            return;
        }

        ClientNationData.NationInfo nation = optNation.get();
        NationRank myRank = nation.members().getOrDefault(localPlayer, NationRank.RECRUIT);

        // Nation header swatch + name
        int swatchColor = 0xFF000000 | nation.color().getFireworkColor();
        g.fill(cx, cy, cx + 24, cy + 24, swatchColor);
        drawBorder(g, cx, cy, 24, 24);
        g.drawString(font, "§f§l" + nation.name(), cx + 28, cy + 4, COLOR_TITLE);
        g.drawString(font, myRank.getColor() + "Your rank: " + myRank.getDisplayName().getString(),
                cx + 28, cy + 14, 0xFFCCCCCC);

        // Leave / Disband button
        boolean isLeader = myRank == NationRank.LEADER;
        String leaveLabel = isLeader ? "§c✕ Disband" : "§e← Leave";
        boolean leaveHover = mx >= cx + cw - 70 && mx < cx + cw && my >= cy + 4 && my < cy + 18;
        g.fill(cx + cw - 70, cy + 4, cx + cw, cy + 18, leaveHover ? 0xFF552222 : 0xFF3A1A1A);
        drawBorder(g, cx + cw - 70, cy + 4, 70, 14);
        g.drawCenteredString(font, leaveLabel, cx + cw - 35, cy + 8, 0xFFFFAAAA);

        int sectionY = cy + 30;

        // ---- Members column ----
        g.drawString(font, "§e§lMembers", cx, sectionY, COLOR_TITLE);
        int memberRowH = 14;
        int maxMemberRows = (ch - 36) / memberRowH;
        List<Map.Entry<UUID, NationRank>> memberEntries = new ArrayList<>(nation.members().entrySet());
        membersScroll = Math.min(membersScroll, Math.max(0, memberEntries.size() - maxMemberRows));

        for (int i = 0; i < maxMemberRows && (i + membersScroll) < memberEntries.size(); i++) {
            Map.Entry<UUID, NationRank> entry = memberEntries.get(i + membersScroll);
            int ry = sectionY + 12 + i * memberRowH;
            NationRank rank = entry.getValue();
            String name = entry.getKey().toString().substring(0, 8) + "…";
            if (entry.getKey().equals(localPlayer)) name = "§n" + name;
            g.drawString(font, rank.getColor() + "[" + rank.name().charAt(0) + "] §f" + name, cx, ry, COLOR_TEXT);
        }

        // ---- Diplomacy column ----
        int splitX = cx + cw / 2 - 4;
        g.drawString(font, "§b§lDiplomacy", splitX + 8, sectionY, COLOR_TITLE);
        List<Map.Entry<UUID, DiplomacyState>> dipEntries = new ArrayList<>(nation.diplomacy().entrySet());
        int dipRowH = 20;
        int maxDipRows = (ch - 36) / dipRowH;
        diplomacyScroll = Math.min(diplomacyScroll, Math.max(0, dipEntries.size() - maxDipRows));

        if (dipEntries.isEmpty()) {
            g.drawString(font, "§7No relations set.", splitX + 8, sectionY + 14, COLOR_SUBTEXT);
        }

        for (int i = 0; i < maxDipRows && (i + diplomacyScroll) < dipEntries.size(); i++) {
            Map.Entry<UUID, DiplomacyState> entry = dipEntries.get(i + diplomacyScroll);
            int ry = sectionY + 12 + i * dipRowH;
            Optional<ClientNationData.NationInfo> other = ClientNationData.getNation(entry.getKey());
            String otherName = other.map(ClientNationData.NationInfo::name).orElse("?");
            DiplomacyState state = entry.getValue();

            int diploColor = switch (state) {
                case ALLIED  -> COLOR_ALLIED;
                case AT_WAR  -> COLOR_WAR;
                case NEUTRAL -> COLOR_NEUTRAL;
            };
            g.drawString(font, "§f" + otherName, splitX + 8, ry, COLOR_TEXT);
            g.drawString(font, state.getColor() + state.getDisplayName().getString(), splitX + 8, ry + 10, diploColor);

            if (myRank == NationRank.LEADER || myRank == NationRank.OFFICER) {
                String nextLabel = switch (state) {
                    case ALLIED  -> "§7Peace";
                    case NEUTRAL -> "§cWar";
                    case AT_WAR  -> "§aAlly";
                };
                int btnX = cx + cw - 36;
                boolean btnHover = mx >= btnX && mx < btnX + 36 && my >= ry && my < ry + 18;
                g.fill(btnX, ry, btnX + 36, ry + 18, btnHover ? 0x88334455 : 0x66223344);
                drawBorder(g, btnX, ry, 36, 18);
                g.drawCenteredString(font, nextLabel, btnX + 18, ry + 5, 0xFFFFFFFF);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Tab 3: Create
    // -----------------------------------------------------------------------

    private void renderCreateTab(GuiGraphics g, int cx, int cy, int cw, int ch, int mx, int my) {
        UUID localPlayer = Minecraft.getInstance().player != null
                ? Minecraft.getInstance().player.getUUID() : null;
        Optional<ClientNationData.NationInfo> myNation = localPlayer != null
                ? ClientNationData.getPlayerNation(localPlayer) : Optional.empty();

        if (myNation.isPresent()) {
            g.drawCenteredString(font, "§cYou are already in a nation.", cx + cw / 2, cy + ch / 2 - 5, 0xFFFF5555);
            g.drawCenteredString(font, "§7Leave your current nation first.", cx + cw / 2, cy + ch / 2 + 8, COLOR_SUBTEXT);
            nameField.setVisible(false);
            createButton.visible = false;
            return;
        }

        nameField.setVisible(true);
        createButton.visible = true;

        nameField.setX(cx);
        nameField.setY(cy + 18);
        nameField.setWidth(cw);
        createButton.setX(cx + cw / 2 - 60);
        createButton.setY(bgY + BG_HEIGHT - 30);

        g.drawString(font, "§eNation Name:", cx, cy + 6, COLOR_TEXT);
        nameField.render(g, mx, my, 0);

        g.drawString(font, "§eChoose Color:", cx, cy + 44, COLOR_TEXT);

        DyeColor[] colors = DyeColor.values();
        int swatchSize = 14;
        int swatchGap = 2;
        int swatchesPerRow = cw / (swatchSize + swatchGap);
        for (int i = 0; i < colors.length; i++) {
            int col = i % swatchesPerRow;
            int row = i / swatchesPerRow;
            int sx = cx + col * (swatchSize + swatchGap);
            int sy = cy + 56 + row * (swatchSize + swatchGap);
            int swatchColor = 0xFF000000 | colors[i].getFireworkColor();
            g.fill(sx, sy, sx + swatchSize, sy + swatchSize, swatchColor);
            if (colors[i] == selectedColor) {
                // Highlight selection with a white border
                g.fill(sx - 1, sy - 1, sx + swatchSize + 1, sy, 0xFFFFFFFF);
                g.fill(sx - 1, sy + swatchSize, sx + swatchSize + 1, sy + swatchSize + 1, 0xFFFFFFFF);
                g.fill(sx - 1, sy, sx, sy + swatchSize, 0xFFFFFFFF);
                g.fill(sx + swatchSize, sy, sx + swatchSize + 1, sy + swatchSize, 0xFFFFFFFF);
            }
        }

        // Preview
        String previewName = nameField.getValue().trim().isEmpty() ? "Your Nation" : nameField.getValue().trim();
        int previewY = cy + 100;
        int previewColor = 0xFF000000 | selectedColor.getFireworkColor();
        g.fill(cx, previewY, cx + 20, previewY + 20, previewColor);
        drawBorder(g, cx, previewY, 20, 20);
        g.drawString(font, "§f§l" + previewName, cx + 24, previewY + 6, COLOR_TEXT);

        createButton.render(g, mx, my, 0);
    }

    // -----------------------------------------------------------------------
    // Input handling (single consolidated mouseClicked override)
    // -----------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        // Tab switching
        Tab[] tabs = Tab.values();
        int tabW = BG_WIDTH / tabs.length;
        int ty = bgY + HEADER_H;
        for (int i = 0; i < tabs.length; i++) {
            int tx = bgX + i * tabW;
            if (mx >= tx && mx < tx + tabW && my >= ty && my < ty + TAB_H) {
                currentTab = tabs[i];
                nationsScroll = 0;
                membersScroll = 0;
                diplomacyScroll = 0;
                return true;
            }
        }

        if (currentTab == Tab.NATIONS && button == 0) {
            handleNationsClick((int) mx, (int) my);
        }

        if (currentTab == Tab.MY_NATION && button == 0) {
            handleMyNationClick((int) mx, (int) my);
        }

        if (currentTab == Tab.CREATE && button == 0) {
            handleCreateTabClick((int) mx, (int) my);
        }

        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (currentTab == Tab.NATIONS) {
            nationsScroll = Math.max(0, nationsScroll - (int) Math.signum(delta));
        } else if (currentTab == Tab.MY_NATION) {
            membersScroll = Math.max(0, membersScroll - (int) Math.signum(delta));
        }
        return super.mouseScrolled(mx, my, delta);
    }

    private void handleNationsClick(int mx, int my) {
        List<ClientNationData.NationInfo> nations = ClientNationData.getNations();
        UUID localPlayer = Minecraft.getInstance().player != null
                ? Minecraft.getInstance().player.getUUID() : null;
        if (localPlayer == null) return;
        if (ClientNationData.getPlayerNation(localPlayer).isPresent()) return;

        int cx = bgX + 6, cy = bgY + CONTENT_Y_START, cw = BG_WIDTH - 12;
        int rowH = 26;
        int visibleRows = (BG_HEIGHT - CONTENT_Y_START - 6) / rowH;

        for (int i = 0; i < visibleRows && (i + nationsScroll) < nations.size(); i++) {
            ClientNationData.NationInfo nation = nations.get(i + nationsScroll);
            int ry = cy + i * rowH;
            if (nation.pendingInvites().contains(localPlayer) &&
                    mx >= cx + cw - 42 && mx < cx + cw && my >= ry + 4 && my < ry + rowH - 4) {
                PacketHandler.sendToServer(new C2SJoinNationPacket(nation.id()));
                return;
            }
        }
    }

    private void handleMyNationClick(int mx, int my) {
        UUID localPlayer = Minecraft.getInstance().player != null
                ? Minecraft.getInstance().player.getUUID() : null;
        if (localPlayer == null) return;
        Optional<ClientNationData.NationInfo> optNation = ClientNationData.getPlayerNation(localPlayer);
        if (optNation.isEmpty()) return;
        ClientNationData.NationInfo nation = optNation.get();
        NationRank myRank = nation.members().getOrDefault(localPlayer, NationRank.RECRUIT);

        int cx = bgX + 6, cy = bgY + CONTENT_Y_START, cw = BG_WIDTH - 12;

        // Leave/Disband button
        if (mx >= cx + cw - 70 && mx < cx + cw && my >= cy + 4 && my < cy + 18) {
            PacketHandler.sendToServer(new C2SLeaveNationPacket());
            this.onClose();
            return;
        }

        // Diplomacy change buttons
        if (myRank == NationRank.LEADER || myRank == NationRank.OFFICER) {
            int sectionY = cy + 30;
            int dipRowH = 20;
            List<Map.Entry<UUID, DiplomacyState>> dipEntries =
                    new ArrayList<>(nation.diplomacy().entrySet());
            int maxDipRows = (BG_HEIGHT - CONTENT_Y_START - 36) / dipRowH;
            diplomacyScroll = Math.min(diplomacyScroll, Math.max(0, dipEntries.size() - maxDipRows));

            for (int i = 0; i < maxDipRows && (i + diplomacyScroll) < dipEntries.size(); i++) {
                Map.Entry<UUID, DiplomacyState> entry = dipEntries.get(i + diplomacyScroll);
                int ry = sectionY + 12 + i * dipRowH;
                int btnX = cx + cw - 36;
                if (mx >= btnX && mx < btnX + 36 && my >= ry && my < ry + 18) {
                    DiplomacyState next = switch (entry.getValue()) {
                        case ALLIED  -> DiplomacyState.NEUTRAL;
                        case NEUTRAL -> DiplomacyState.AT_WAR;
                        case AT_WAR  -> DiplomacyState.ALLIED;
                    };
                    PacketHandler.sendToServer(new C2SDiplomacyPacket(entry.getKey(), next));
                    return;
                }
            }
        }
    }

    private void handleCreateTabClick(int mx, int my) {
        int cx = bgX + 6, cy = bgY + CONTENT_Y_START, cw = BG_WIDTH - 12;
        DyeColor[] colors = DyeColor.values();
        int swatchSize = 14, swatchGap = 2;
        int swatchesPerRow = cw / (swatchSize + swatchGap);
        for (int i = 0; i < colors.length; i++) {
            int col = i % swatchesPerRow;
            int row = i / swatchesPerRow;
            int sx = cx + col * (swatchSize + swatchGap);
            int sy = cy + 56 + row * (swatchSize + swatchGap);
            if (mx >= sx && mx < sx + swatchSize && my >= sy && my < sy + swatchSize) {
                selectedColor = colors[i];
                return;
            }
        }
    }

    private void doCreateNation() {
        String name = nameField.getValue().trim();
        if (name.length() < 3 || name.length() > 24) return;
        PacketHandler.sendToServer(new C2SCreateNationPacket(name, selectedColor));
        this.onClose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (currentTab == Tab.CREATE && nameField.isFocused()) {
            return nameField.keyPressed(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (currentTab == Tab.CREATE && nameField.isFocused()) {
            return nameField.charTyped(c, modifiers);
        }
        return super.charTyped(c, modifiers);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private void drawBorder(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + 1, COLOR_BORDER);
        g.fill(x, y + h - 1, x + w, y + h, COLOR_BORDER);
        g.fill(x, y, x + 1, y + h, COLOR_BORDER);
        g.fill(x + w - 1, y, x + w, y + h, COLOR_BORDER);
    }

    private String translate(String key) {
        return net.minecraft.locale.Language.getInstance().getOrDefault(key, key);
    }
}
