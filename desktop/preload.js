const {contextBridge,ipcRenderer}=require('electron');
const fs=require('fs'),path=require('path');
const {app}=require('electron');

function file(){return path.join(app.getPath('userData'),'kaoqin-data.json')}
function read(){
  try{
    const d=JSON.parse(fs.readFileSync(file(),'utf8'));
    return {people:Array.isArray(d.people)?d.people:[],notes:d.notes&&typeof d.notes==='object'?d.notes:{},manual:d.manual&&typeof d.manual==='object'?d.manual:{}};
  }catch(e){return {people:[],notes:{},manual:{}}}
}
function write(d){fs.mkdirSync(path.dirname(file()),{recursive:true});fs.writeFileSync(file(),JSON.stringify(d,null,2),'utf8')}
function saveNote(key,value){const d=read();d.notes[key]=value;d.manual['manual_'+key]=true;d.manual[key]=true;write(d)}
function removeNote(key){const d=read();delete d.notes[key];delete d.manual[key];delete d.manual['manual_'+key];write(d)}
function isManualNote(key){const d=read();return !!(d.manual[key]||d.manual['manual_'+key])}
function savePerson(id,name,restWeekdays){
  const d=read();let found=false;
  d.people=d.people.map(p=>{if(p.id===id){found=true;return {...p,name,restWeekdays}}return p});
  if(!found)d.people.push({id,name,restWeekdays});
  write(d);return true;
}
function deletePerson(id){
  const d=read();d.people=d.people.filter(p=>p.id!==id);
  const prefix='multi_note_'+id+'_';
  for(const k of Object.keys(d.notes)){if(k.startsWith(prefix))delete d.notes[k]}
  for(const k of Object.keys(d.manual)){if(k.startsWith(prefix)||k.startsWith('manual_'+prefix))delete d.manual[k]}
  write(d);return true;
}
contextBridge.exposeInMainWorld('AndroidStorage',{
  getNote:(key)=>{const d=read();return Object.prototype.hasOwnProperty.call(d.notes,key)?d.notes[key]:null},
  saveNote,removeNote,isManualNote,
  getPeople:()=>JSON.stringify(read().people),
  savePerson,deletePerson
});
ipcRenderer.on('attendance-updated',()=>window.dispatchEvent(new Event('attendance-updated')));
