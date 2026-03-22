import json
import sys
import math
import os
from collections import deque, Counter

import pygame           # pygame is our visualiser
                        # sounds crazy but im serious
import matplotlib
matplotlib.use("Agg")   # renders to image, not a window
import matplotlib.pyplot as plt
import matplotlib.patches as mpatches
import numpy as np

JSON_PATH = "rbt_export.json"

# pygame variables
BG_COLOR = (18, 18, 28) # these are just rgb values
PANEL_COLOR = (28, 28, 44)
PANEL_BORDER = (60, 60, 90)

RED_NODE = (210, 60, 60)
RED_NODE_HL = (255, 100, 100)

BLACK_NODE = (60, 60, 80)
BLACK_NODE_HL = (110, 110, 150)

EDGE_COLOR = (80, 80, 110)
TEXT_COLOR = (220, 220, 240)
DIM_TEXT = (130, 130, 160)
ACCENT = (100, 180, 255)
GOLD = (255, 210, 80)

NODE_RADIUS = 7
MIN_ZOOM = 0.08
MAX_ZOOM = 6.0
INITIAL_ZOOM = 0.55

WINDOW_W = 1400
WINDOW_H = 860
INFO_W = 320
CHART_REFRESH_S = 0.4 # re-rendering

def load_tree(path):
  with open(path) as f:
    data = json.load(f)
  meta = data["metadata"]
  nodes = {n["id"]: n for n in data["nodes"]}
  root_id = data["nodes"][0]["id"]
  return meta, nodes, root_id

# rbt visual layout
def layout(nodes, root_id):
    # in-order traversal
    x_counter = [0]
    positions = {}

    def inorder(nid):
        if nid == -1: return
        n = nodes[nid]
        inorder(n["left_id"])
        positions[nid] = [x_counter[0], 0]
        x_counter[0] += 1
        inorder(n["right_id"])

    inorder(root_id)

    # breadth first-search
    q = deque([(root_id, 0)])
    while q:
        nid, depth = q.popleft()
        positions[nid][1] = depth
        n = nodes[nid]
        if n["left_id"]  != -1:
            q.append((n["left_id"],  depth + 1))
        if n["right_id"] != -1:
            q.append((n["right_id"], depth + 1))
 
    # normalise so root is centred at (0, 0)
    xs = [p[0] for p in positions.values()]
    ys = [p[1] for p in positions.values()]
    cx = (min(xs) + max(xs)) / 2

    # node spacing
    H_GAP = 1.8
    V_GAP = 40
 
    for nid, (px, py) in positions.items():
        positions[nid] = ((px - cx) * H_GAP, py * V_GAP)
 
    return positions

def render_charts(meta, nodes, root_id):
    node_list = list(nodes.values())

    # red-black per depth
    id_to_depth = {}
    q = deque([(root_id, 0)])
    while q:
        nid, depth = q.popleft()
        id_to_depth[nid] = depth
        n = nodes[nid]
        if n["left_id"]  != -1:
            q.append((n["left_id"],  depth + 1))
        if n["right_id"] != -1:
            q.append((n["right_id"], depth + 1))

    max_depth = max(id_to_depth.values())
    red_per_depth = [0] * (max_depth + 1)
    black_per_depth = [0] * (max_depth + 1)
    for nid, depth in id_to_depth.items():
        if nodes[nid]["color"] == "RED":
            red_per_depth[depth] += 1
        else:
            black_per_depth[depth] += 1

    # orbital period histogram
    periods = [n["orbital_period"] for n in node_list if n["orbital_period"]]
    log_periods = np.log10(periods)

    # discovery method
    methods = Counter(n["discovery_method"] or "Unknown" for n in node_list)
    method_labels = [k for k, _ in methods.most_common()]
    method_counts = [v for _, v in methods.most_common()]

    # height vs theoretical max per subtree sample
    depths = list(id_to_depth.values())
    depth_counts = Counter(depths)
    depth_x = sorted(depth_counts)
    depth_y = [depth_counts[d] for d in depth_x]

    # plots
    fig, axes = plt.subplots(2, 2, figsize=(11, 7.5), facecolor="#12121c")
    fig.suptitle("Red-Black Tree — NASA Planetary Systems Analysis",
                color="#e0e0f0", fontsize=13, fontweight="bold", y=0.98)

    style = dict(facecolor="#1c1c2c", edgecolor="#3c3c5c",
                tick_params=dict(colors="#9090b0"),
                label_color="#c0c0d8", title_color="#e0e0f0")

    def style_ax(ax, title):
        ax.set_facecolor(style["facecolor"])
        
        for spine in ax.spines.values():
            spine.set_edgecolor(style["edgecolor"])
            
        ax.tick_params(colors=style["tick_params"]["colors"], labelsize=8)
        ax.set_title(title, color=style["title_color"], fontsize=10, pad=6)
        ax.xaxis.label.set_color(style["label_color"])
        ax.yaxis.label.set_color(style["label_color"])

    # plot 1: red-black nodes per depth
    ax = axes[0][0]
    depth_range = range(max_depth + 1)
    ax.bar(depth_range, black_per_depth, label="Black", color="#4a4a6a", width=0.8)
    ax.bar(depth_range, red_per_depth,   label="Red",   color="#c03c3c", 
           bottom=black_per_depth, width=0.8)
    ax.set_xlabel("Depth")
    ax.set_ylabel("Node Count")
    ax.legend(facecolor="#1c1c2c", edgecolor="#3c3c5c",
            labelcolor="#c0c0d8", fontsize=8)
    style_ax(ax, "Red / Black Nodes per Depth Level")

    # plot 2: orbital period histogram
    ax = axes[0][1]
    ax.hist(log_periods, bins=50, color="#4a90d9", edgecolor="#1c1c2c", linewidth=0.4)
    ax.set_xlabel("log₁₀(Orbital Period / days)")
    ax.set_ylabel("Count")

    for xv, lbl, col in [(math.log10(10), "10 d", "#ff9944"),
                        (math.log10(365), "1 yr",  "#88dd88"),
                        (math.log10(4333), "Jupiter", "#dd88ff")]:
        ax.axvline(xv, color=col, linewidth=1, linestyle="--", alpha=0.7)
        ax.text(xv + 0.03, ax.get_ylim()[1] * 0.85, lbl,
                color=col, fontsize=7, va="top")
    style_ax(ax, "Orbital Period Distribution (log scale)")

    # plot 3: discovery method breakdown
    ax = axes[1][0]
    colors_bar = plt.cm.Set2(np.linspace(0, 1, len(method_labels)))
    bars = ax.barh(method_labels[::-1], method_counts[::-1], color=colors_bar[::-1], edgecolor="#1c1c2c", linewidth=0.4)
    
    for bar, count in zip(bars, method_counts[::-1]):
        ax.text(bar.get_width() + 20, bar.get_y() + bar.get_height() / 2, f"{count:,}", va="center", color="#c0c0d8", fontsize=7)
        
    ax.set_xlabel("Number of Planets")
    ax.set_xlim(0, max(method_counts) * 1.15)
    style_ax(ax, "Discovery Method Breakdown")

    # plot 4: nodes per depth and theoretical perfect binary tree
    ax = axes[1][1]
    theoretical = [min(2**d, len(nodes)) for d in depth_x]
    
    ax.plot(depth_x, depth_y,       color="#4a90d9", linewidth=2, marker="o", markersize=4, label="Actual")
    ax.plot(depth_x, theoretical,   color="#88cc66", linewidth=1.5, linestyle="--", label="Perfect (2ⁿ cap)")
    
    ax.set_xlabel("Depth")
    ax.set_ylabel("Node Count")
    ax.legend(facecolor="#1c1c2c", edgecolor="#3c3c5c", labelcolor="#c0c0d8", fontsize=8)
    style_ax(ax, "Nodes per Depth vs Perfect Binary Tree")

    # metadata text strip
    fig.text(0.5, 0.01,
        f"Size: {meta['size']:,}   Height: {meta['height']}   "
        f"Black-height: {meta['black_height']}   "
        f"Rotations: {meta['rotations']:,}   "
        f"Recolourings: {meta['recolorings']:,}",
        ha="center", color="#7070a0", fontsize=8)

    plt.tight_layout(rect=[0, 0.03, 1, 0.96])

    # actual render to pygame
    fig.canvas.draw()
    w, h = fig.canvas.get_width_height()
    import numpy as np
    buf = np.frombuffer(fig.canvas.buffer_rgba(), dtype=np.uint8).reshape(h, w, 4)
    plt.close(fig)
    surface = pygame.surfarray.make_surface(buf[:, :, :3].transpose(1, 0, 2))
    return surface

def draw_info_panel(screen, node, font_sm, font_md, font_lg, x, y, w, h):
    """Draw the planet detail panel on the right side."""
    pygame.draw.rect(screen, PANEL_COLOR, (x, y, w, h))
    pygame.draw.rect(screen, PANEL_BORDER, (x, y, w, h), 1)
 
    pad = 14
    cy  = y + pad
 
    def line(text, font, color=TEXT_COLOR, indent=0):
        nonlocal cy
        surf = font.render(text, True, color)
        screen.blit(surf, (x + pad + indent, cy))
        cy += surf.get_height() + 4
 
    def divider():
        nonlocal cy
        pygame.draw.line(screen, PANEL_BORDER,
                         (x + pad, cy), (x + w - pad, cy))
        cy += 8
 
    if node is None:
        line("Hover a node", font_md, DIM_TEXT)
        line("to see details", font_md, DIM_TEXT)
        return
 
    color_label = "RED" if node["color"] == "RED" else "BLACK"
    node_col    = RED_NODE if node["color"] == "RED" else (150, 150, 180)
 
    line(node["planet_name"] or "—", font_lg, ACCENT)
    line(f"Node colour: {color_label}", font_sm, node_col)
    divider()
 
    line("ORBITAL", font_sm, GOLD)
    line(f"Period:  {node['orbital_period']:.4f} days", font_sm, indent=8)
    cy += 2
    divider()
 
    line("PLANET", font_sm, GOLD)
    re = node.get("radius_earth")
    me = node.get("mass_earth")
    line(f"Radius:  {re:.2f} R⊕"  if re else "Radius:  —", font_sm, indent=8)
    line(f"Mass:    {me:.2f} M⊕"  if me else "Mass:    —", font_sm, indent=8)
    cy += 2
    divider()
 
    line("STAR", font_sm, GOLD)
    line(f"Host:    {node['host_name'] or '—'}", font_sm, indent=8)
    cy += 2
    divider()
 
    line("DISCOVERY", font_sm, GOLD)
    line(f"Year:    {node['discovery_year'] or '—'}", font_sm, indent=8)
    method = node.get("discovery_method") or "—"
    # wrap long method names
    if len(method) > 22:
        line(f"Method:", font_sm, indent=8)
        line(f"  {method}", font_sm, indent=8)
    else:
        line(f"Method:  {method}", font_sm, indent=8)
    dist = node.get("distance_pc")
    line(f"Distance:{dist:.1f} pc" if dist else "Distance: —", font_sm, indent=8)
    cy += 2
    divider()
 
    line("RBT NODE", font_sm, GOLD)
    line(f"ID: {node['id']}", font_sm, DIM_TEXT, indent=8)
    left  = node["left_id"]
    right = node["right_id"]
    line(f"Left:  {'NIL' if left  == -1 else left}",  font_sm, DIM_TEXT, indent=8)
    line(f"Right: {'NIL' if right == -1 else right}", font_sm, DIM_TEXT, indent=8)
 
 
def main():
    path = sys.argv[1] if len(sys.argv) > 1 else JSON_PATH
    if not os.path.exists(path):
        print(f"Cannot find JSON file: {path}")
        print("Usage: python visualize.py <rbt_export.json>")
        sys.exit(1)
 
    print("Loading tree data...")
    meta, nodes, root_id = load_tree(path)
    print(f"  {len(nodes):,} nodes loaded.")
 
    print("Computing layout...")
    positions = layout(nodes, root_id)
    print("  Layout done.")
 
    # pygame launch
    pygame.init()
    screen = pygame.display.set_mode((WINDOW_W, WINDOW_H), pygame.RESIZABLE)
    pygame.display.set_caption("Red-Black Tree — NASA Planetary Systems")
    clock  = pygame.time.Clock()
 
    font_lg = pygame.font.SysFont("segoeui",   15, bold=True)
    font_md = pygame.font.SysFont("segoeui",   13)
    font_sm = pygame.font.SysFont("segoeui",   11)
    font_xs = pygame.font.SysFont("consolas",  10)
 
    # set pygame init settings
    zoom        = INITIAL_ZOOM
    offset_x    = WINDOW_W // 2 - INFO_W // 2
    offset_y    = 60
    dragging    = False
    drag_start  = (0, 0)
    drag_origin = (0, 0)
 
    hovered_id  = None
    pinned_id   = None
 
    show_charts    = False
    chart_surface  = None
    chart_dirty    = True
    last_chart_t   = 0.0
 
    # precompute edge list once
    edges = []
    for nid, n in nodes.items():
        if n["left_id"]  != -1: edges.append((nid, n["left_id"]))
        if n["right_id"] != -1: edges.append((nid, n["right_id"]))
 
    def world_to_screen(wx, wy):
        return (int(wx * zoom + offset_x),
                int(wy * zoom + offset_y))
 
    def screen_to_world(sx, sy):
        return ((sx - offset_x) / zoom,
                (sy - offset_y) / zoom)
 
    def node_screen_pos(nid):
        wx, wy = positions[nid]
        return world_to_screen(wx, wy)
 
    running = True
    while running:
        dt = clock.tick(60) / 1000.0
        sw, sh = screen.get_size()
        tree_w  = sw - INFO_W
 
        # events
        for event in pygame.event.get():
            if event.type == pygame.QUIT:
                running = False
 
            elif event.type == pygame.KEYDOWN:
                if event.key in (pygame.K_q, pygame.K_ESCAPE):
                    running = False
                elif event.key == pygame.K_r:
                    zoom     = INITIAL_ZOOM
                    offset_x = sw // 2 - INFO_W // 2
                    offset_y = 60
                elif event.key == pygame.K_c:
                    show_charts = not show_charts
                    chart_dirty = True
 
            elif event.type == pygame.MOUSEBUTTONDOWN:
                mx, my = event.pos
                if event.button == 1 and mx < tree_w:
                    dragging    = True
                    drag_start  = (mx, my)
                    drag_origin = (offset_x, offset_y)
                    # click to pin/unpin
                    if hovered_id is not None:
                        pinned_id = None if pinned_id == hovered_id else hovered_id
                elif event.button == 4: # scroll up to zoom in
                    mx, my = event.pos
                    factor = 1.12
                    wx, wy = screen_to_world(mx, my)
                    zoom = min(MAX_ZOOM, zoom * factor)
                    offset_x = mx - wx * zoom
                    offset_y = my - wy * zoom
                elif event.button == 5: # scroll down to zoom out
                    mx, my = event.pos
                    factor = 1.12
                    wx, wy = screen_to_world(mx, my)
                    zoom = max(MIN_ZOOM, zoom / factor)
                    offset_x = mx - wx * zoom
                    offset_y = my - wy * zoom
 
            elif event.type == pygame.MOUSEBUTTONUP:
                if event.button == 1:
                    dragging = False
 
            elif event.type == pygame.MOUSEMOTION:
                if dragging:
                    mx, my = event.pos
                    offset_x = drag_origin[0] + (mx - drag_start[0])
                    offset_y = drag_origin[1] + (my - drag_start[1])
 
            elif event.type == pygame.VIDEORESIZE:
                sw, sh = event.w, event.h
                tree_w  = sw - INFO_W
 
        # hover direction
        if not dragging:
            mx, my = pygame.mouse.get_pos()
            hovered_id = None
            if mx < tree_w:
                r = max(4, int(NODE_RADIUS * zoom))
                for nid, (wx, wy) in positions.items():
                    sx, sy = world_to_screen(wx, wy)
                    if abs(sx - mx) < r + 2 and abs(sy - my) < r + 2:
                        hovered_id = nid
                        break
 
        active_id = pinned_id if pinned_id is not None else hovered_id
 
        # draw on screen
        screen.fill(BG_COLOR)
 
        # clip tree area
        tree_rect = pygame.Rect(0, 0, tree_w, sh)
        screen.set_clip(tree_rect)
 
        r = max(2, int(NODE_RADIUS * zoom))
 
        # edges — skip if too small to see
        if zoom > 0.12:
            for (aid, bid) in edges:
                ax2, ay2 = node_screen_pos(aid)
                bx2, by2 = node_screen_pos(bid)
                # cull off-screen edges
                if (max(ax2, bx2) < -20 or min(ax2, bx2) > tree_w + 20 or
                    max(ay2, by2) < -20 or min(ay2, by2) > sh + 20):
                    continue
                pygame.draw.line(screen, EDGE_COLOR, (ax2, ay2), (bx2, by2), 1)
 
        # nodes
        for nid, (wx, wy) in positions.items():
            sx, sy = world_to_screen(wx, wy)
            if sx < -r or sx > tree_w + r or sy < -r or sy > sh + r:
                continue
 
            is_red  = nodes[nid]["color"] == "RED"
            is_active = (nid == active_id)
 
            base_col = RED_NODE    if is_red else BLACK_NODE
            hl_col   = RED_NODE_HL if is_red else BLACK_NODE_HL
 
            col = hl_col if is_active else base_col
            draw_r = r + 2 if is_active else r
 
            pygame.draw.circle(screen, col, (sx, sy), draw_r)
            if is_active:
                pygame.draw.circle(screen, GOLD, (sx, sy), draw_r + 2, 2)
            elif zoom > 1.2:
                border = (180, 80, 80) if is_red else (90, 90, 120)
                pygame.draw.circle(screen, border, (sx, sy), r, 1)
 
            # label when zoomed in enough
            if zoom > 2.5 and r > 8:
                name = nodes[nid]["planet_name"] or ""
                short = name[:12] + "…" if len(name) > 12 else name
                lbl = font_xs.render(short, True, TEXT_COLOR)
                screen.blit(lbl, (sx - lbl.get_width() // 2, sy + r + 2))
 
        screen.set_clip(None)
 
        # info panel
        panel_x = sw - INFO_W
        draw_info_panel(screen, nodes.get(active_id), font_sm, font_md, font_lg,
                        panel_x, 0, INFO_W, sh)
 
        # charts
        if show_charts:
            now = pygame.time.get_ticks() / 1000.0
            if chart_dirty or chart_surface is None:
                chart_surface = render_charts(meta, nodes, root_id)
                chart_dirty   = False
                last_chart_t  = now
 
            # scale chart to fit inside tree area
            cw = min(tree_w - 40, chart_surface.get_width())
            ch = int(cw * chart_surface.get_height() / chart_surface.get_width())
            ch = min(ch, sh - 40)
            cw = int(ch * chart_surface.get_width() / chart_surface.get_height())
 
            scaled = pygame.transform.smoothscale(chart_surface, (cw, ch))
            cx2 = (tree_w - cw) // 2
            cy2 = (sh    - ch) // 2
 
            # semi-transparent backdrop
            backdrop = pygame.Surface((cw + 20, ch + 20), pygame.SRCALPHA)
            backdrop.fill((10, 10, 20, 220))
            screen.blit(backdrop, (cx2 - 10, cy2 - 10))
            screen.blit(scaled, (cx2, cy2))
 
            # close hint
            hint = font_sm.render("Press C to close charts", True, DIM_TEXT)
            screen.blit(hint, (cx2 + cw - hint.get_width() - 4,
                               cy2 + ch + 4))
 
        # hud
        hud_lines = [
            f"Nodes: {len(nodes):,}   Height: {meta['height']}   "
            f"Black-height: {meta['black_height']}   "
            f"Zoom: {zoom:.2f}×",
            "Drag to pan  |  Scroll to zoom  |  Hover node for details  |  "
            "C = charts  |  R = reset  |  Q = quit"
        ]
        for i, txt in enumerate(hud_lines):
            s = font_sm.render(txt, True, DIM_TEXT)
            screen.blit(s, (10, 6 + i * 16))
 
        if pinned_id is not None:
            pin_txt = font_sm.render("● Pinned — click node to unpin", True, GOLD)
            screen.blit(pin_txt, (10, sh - 20))
 
        pygame.display.flip()
 
    pygame.quit()
 
if __name__ == "__main__":
    main()