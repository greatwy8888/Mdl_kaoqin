const {app,BrowserWindow}=require('electron');
const path=require('path');
const fs=require('fs');
const DATA_FILE=()=>path.join(app.getPath('userData'),'kaoqin-data.json');
function readData(){try{const d=JSON.parse(fs.readFileSync(DATA_FILE(),'utf8'));return {people:Array.isArray(d.people)?d.people:[],notes:d.notes&&typeof d.notes==='object'?d.notes:{},manual:d.manual&&typeof d.manual==='object'?d.manual:{}}}catch(e){return {people:[],notes:{},manual:{}}}}
function writeData(d){fs.mkdirSync(path.dirname(DATA_FILE()),{recursive:true});fs.writeFileSync(DATA_FILE(),JSON.stringify(d,null,2),'utf8')}
function weekdayOf(y,m,d){return ((new Date(y,m-1,d).getDay()+6)%7)+1}
function fillToday(){const d=readData(),now=new Date(),y=now.getFullYear(),m=now.getMonth()+1,day=now.getDate(),w=weekdayOf(y,m,day),date=y+'-'+String(m).padStart(2,'0')+'-'+String(day).padStart(2,'0');for(const p of d.people){if(!p||!p.id||d.manual['multi_note_'+p.id+'_'+date])continue;let rest=[];try{rest=Array.isArray(p.restWeekdays)?p.restWeekdays.map(Number):JSON.parse(p.restWeekdays||'[]').map(Number)}catch(e){}const k='multi_note_'+p.id+'_'+date;d.notes[k]=rest.includes(w)?'休息':'正常';d.manual[k]=false}writeData(d)}
let win=null,timer=null;
function schedule(){if(timer)clearTimeout(timer);const now=new Date(),next=new Date(now);next.setHours(1,0,0,0);if(next<=now)next.setDate(next.getDate()+1);timer=setTimeout(()=>{fillToday();if(win)win.webContents.send('attendance-updated');schedule()},Math.max(1000,next-now))}
function createWindow(){win=new BrowserWindow({width:430,height:900,minWidth:360,minHeight:700,autoHideMenuBar:true,webPreferences:{preload:path.join(__dirname,'preload.js'),contextIsolation:true,nodeIntegration:false}});win.loadFile(path.join(__dirname,'index.html'));win.on('closed',()=>{win=null})}
app.whenReady().then(()=>{fillToday();createWindow();schedule()});
app.on('window-all-closed',()=>{if(process.platform!=='darwin')app.quit()});
app.on('activate',()=>{if(!win)createWindow()});
