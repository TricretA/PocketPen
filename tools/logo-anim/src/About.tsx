import React from 'react';
import {AbsoluteFill, Easing, interpolate, spring, useCurrentFrame, useVideoConfig} from 'remotion';
import {WORDMARK} from './wordmark';

const INK = '#12141B';
const SIGNAL = '#F04A0C';
const LIP = '#B93405';
const PAPER = '#F7F4ED';
const OUTER = 'M80 28H176A52 52 0 0 1 228 80V176A52 52 0 0 1 176 228H80A52 52 0 0 1 28 176V80A52 52 0 0 1 80 28Z';
const NIB = 'M122 204V137.5A13 13 0 1 1 134 137.5V204L176 132C184 108 168 74 128 74C88 74 72 108 80 132Z';
const NIB_LENGTH = 700;

const clamp = {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'} as const;

export const About: React.FC = () => {
  const frame = useCurrentFrame();
  const {fps} = useVideoConfig();

  const pop = spring({frame: frame - 2, fps, config: {damping: 11, stiffness: 140, mass: 0.7}});
  const scale = interpolate(pop, [0, 1], [0.35, 1]);
  const opacity = interpolate(frame, [0, 6], [0, 1], clamp);
  const fadeOut = interpolate(frame, [100, 108], [1, 0], clamp);

  const draw = interpolate(frame, [14, 36], [0, 1], {...clamp, easing: Easing.out(Easing.cubic)});
  const fill = interpolate(frame, [32, 42], [0, 1], clamp);

  const press = interpolate(frame, [44, 49, 56], [0, 1, 0], {...clamp, easing: Easing.inOut(Easing.quad)});
  const lip = 10 * (1 - press);
  const shift = 10 * press;

  const drop = interpolate(frame, [36, 46, 54], [0, 1, 1], clamp);
  const dropY = interpolate(frame, [36, 48], [-22, 0], {...clamp, easing: Easing.bounce});
  const ring = interpolate(frame, [48, 66], [0, 1], {...clamp, easing: Easing.out(Easing.cubic)});

  const wordProgress = interpolate(frame, [52, 70], [0, 1], {...clamp, easing: Easing.out(Easing.cubic)});
  const wordY = interpolate(wordProgress, [0, 1], [34, 0]);
  const line = interpolate(frame, [64, 80], [0, 1], {...clamp, easing: Easing.inOut(Easing.cubic)});

  const markSize = 300;
  const centerY = 250;
  const markScale = markSize / 200;

  return (
    <AbsoluteFill style={{backgroundColor: INK}}>
      <AbsoluteFill
        style={{
          background: `radial-gradient(circle at 50% 36%, rgba(240,74,12,${0.16 * opacity * fadeOut}) 0%, rgba(18,20,27,0) 55%)`,
        }}
      />
      <svg width="720" height="720" viewBox="0 0 720 720" style={{position: 'absolute', inset: 0}} opacity={fadeOut}>
        <g transform={`translate(360 ${centerY}) scale(${scale}) translate(${-markSize / 2} ${-markSize / 2})`} opacity={opacity}>
          <g transform={`translate(0 ${shift}) scale(${markScale}) translate(-28 -28)`}>
            <path d={OUTER} fill={LIP} transform={`translate(0 ${lip})`} />
            <path d={OUTER} fill={SIGNAL} />
            <path
              d={NIB}
              fill="none"
              stroke={INK}
              strokeWidth={7}
              strokeLinejoin="round"
              strokeLinecap="round"
              strokeDasharray={NIB_LENGTH}
              strokeDashoffset={NIB_LENGTH * (1 - draw)}
            />
            <path d={NIB} fill={INK} opacity={fill} />
            <circle cx={128} cy={216 + dropY} r={9 * drop} fill={PAPER} />
          </g>
        </g>
        <circle
          cx={360}
          cy={centerY + markSize / 2 + 14}
          r={22 + 110 * ring}
          fill="none"
          stroke={SIGNAL}
          strokeWidth={3}
          opacity={(1 - ring) * 0.7 * (frame > 46 ? 1 : 0)}
        />
        <g transform={`translate(${360 - (746 * 0.371) / 2} ${517 + wordY * 0.7}) scale(0.371)`} opacity={wordProgress}>
          <g transform="translate(0 79)">
            <path d={WORDMARK} fill={PAPER} fillRule="nonzero" transform="translate(0 0)" />
          </g>
        </g>
        <rect x={360 - 110 * line} y={593} width={220 * line} height={4} rx={2} fill={SIGNAL} />
      </svg>
    </AbsoluteFill>
  );
};
