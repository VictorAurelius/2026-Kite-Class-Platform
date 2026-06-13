/** Capture bìa thesis VỚI page border (docx-preview không vẽ pgBorders → inject CSS khớp). */
const fs=require('fs'),path=require('path');
const {chromium}=require('/home/kitedev/projects/2026-Kite-Class-Platform/kiteclass/kiteclass-frontend/node_modules/@playwright/test');
const T='/home/kitedev/projects/2026-Kite-Class-Platform/documents/08-thesis';
(async()=>{const b64=fs.readFileSync(path.join(T,'thesis-v1.docx')).toString('base64');
const br=await chromium.launch();const pg=await br.newPage({deviceScaleFactor:2});
await pg.setContent('<!doctype html><body style="margin:0;background:#fff"><div id="c"></div></body>');
await pg.addScriptTag({url:'https://unpkg.com/jszip@3.10.1/dist/jszip.min.js'});
await pg.addScriptTag({url:'https://unpkg.com/docx-preview@0.3.5/dist/docx-preview.min.js'});
await pg.evaluate(async b64=>{const bin=atob(b64),a=new Uint8Array(bin.length);for(let i=0;i<bin.length;i++)a[i]=bin.charCodeAt(i);await window.docx.renderAsync(new Blob([a]),document.getElementById('c'),null,{className:'docx',inWrapper:true,breakPages:true,experimental:true});},b64);
await pg.waitForTimeout(2000);
// Inject page border on cover sections [0]+[1] khớp pgBorders docx (single 3pt đen, space ~24pt từ mép)
await pg.addStyleTag({content:`.docx-wrapper > section:nth-child(1), .docx-wrapper > section:nth-child(2){outline:3px solid #000; outline-offset:-30px;}`});
await pg.waitForTimeout(300);
const secs=await pg.$$('.docx-wrapper > section');
await secs[0].screenshot({path:path.join(T,'screenshots-render','wave4-cover-bordered.png')});
console.log('saved wave4-cover-bordered.png');
await br.close();})().catch(e=>{console.error(e.message);process.exit(1)});
